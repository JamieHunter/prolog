// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.bootstrap;

import org.jprolog.constants.PrologAtomInterned;
import org.jprolog.execution.CompileContext;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.Term;
import org.jprolog.functions.StackFunction;
import org.jprolog.library.Library;
import org.jprolog.predicates.BuiltInPredicate;
import org.jprolog.predicates.BuiltinPredicateArity0;
import org.jprolog.predicates.BuiltinPredicateArity1;
import org.jprolog.predicates.BuiltinPredicateArity2;
import org.jprolog.predicates.BuiltinPredicateArity3;
import org.jprolog.predicates.BuiltinPredicateArity4;
import org.jprolog.predicates.BuiltinPredicateArity5;
import org.jprolog.predicates.BuiltinPredicateCompiles;
import org.jprolog.predicates.Predication;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Base class to consult a Java package through reflection. See {@link Library} for example of use.
 * Each such library should be referenced by {@link Builtins}.
 */
public class LibraryBase {

    //
    // Mapping table, given an arity of N, identify an optimal pair of classes to use for the Lambda mappings
    //
    private static final Entry[] byArity = {
            new Entry(BuiltinPredicateArity0.class, BuiltinPredicateArity0.Lambda.class),
            new Entry(BuiltinPredicateArity1.class, BuiltinPredicateArity1.Lambda.class),
            new Entry(BuiltinPredicateArity2.class, BuiltinPredicateArity2.Lambda.class),
            new Entry(BuiltinPredicateArity3.class, BuiltinPredicateArity3.Lambda.class),
            new Entry(BuiltinPredicateArity4.class, BuiltinPredicateArity4.Lambda.class),
            new Entry(BuiltinPredicateArity5.class, BuiltinPredicateArity5.Lambda.class),
            // Extend as needed
    };

    /**
     * Consult a specific class to library by identifying all method and field predicates. The class is treated as
     * a utility class.
     *
     * @param cls Java Class to consult.
     */
    protected void consult(Class<?> cls) {
        // force-load
        try {
            Class.forName(cls.getName()); // forces a class to be initialized
        } catch (ClassNotFoundException e) {
            throw new InternalError(e);
        }
        for (Method m : cls.getMethods()) {
            //
            // Predicate described by a method - either execution style,
            // or compilation style. The style can be inferred by the first parameter.
            //
            Predicate predicate = m.getAnnotation(Predicate.class);
            if (predicate != null) {
                handlePredicateMethod(predicate, m);
            }
        }
        for (Field f : cls.getDeclaredFields()) {
            //
            // Predicate/0 described by a singleton.
            //
            Predicate predicate = f.getAnnotation(Predicate.class);
            if (predicate != null) {
                handlePredicateField(predicate, f);
            }
            //
            // Predicate/2 described by a singleton StackFunction to implement compare.
            //
            Compare compare = f.getAnnotation(Compare.class);
            if (compare != null) {
                // Special predicate
                handleComparePredicateField(compare, f);
            }
            //
            // Arithmetic function described by a singleton StackFunction to implement an operation.
            //
            Function function = f.getAnnotation(Function.class);
            if (function != null) {
                handleFunctionField(function, f);
            }
            //
            // A set of predications that are defined by a Java Resource, loaded via the LoadResourceOnDemand
            // mini loader. These are used as part of the bootstrap process, on demand.
            //
            DemandLoad demandLoad = f.getAnnotation(DemandLoad.class);
            if (demandLoad != null) {
                handleDemandLoadField(demandLoad, f);
            }
        }
    }

    /**
     * Helper, ensures parameter type is exactly as described.
     *
     * @param actual   Actual parameter per reflection
     * @param expected Parameter type expected
     * @return true if match
     */
    private static boolean isParamType(Class<?> actual, Class<?> expected) {
        return (expected.isAssignableFrom(actual) && actual.isAssignableFrom(expected));
    }

    /**
     * Create a brief descriptive string for the member (vs member.toString())
     *
     * @param member Reflected member
     * @return Brief descriptive string
     */
    private static String describe(Member member, String... names) {
        StringBuilder builder = new StringBuilder();
        builder.append(member.getDeclaringClass().getName());
        builder.append('.');
        builder.append(member.getName());
        builder.append("[");
        if (names.length > 0) {
            builder.append(names[0]);
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * Handle a method annotated with {@link Predicate}. There are two forms supported. The first, with parameter type
     * {@link Environment} is a static executable method. The second, with parameter type {@link CompileContext} is
     * a compile-time method. The first is used for simple execute-time functionality. The second is used to support
     * compile-time optimization that inserts one or more {@link Instruction} objects into the compiled block.
     *
     * @param predicate Predicate Annotation
     * @param method    Reflected method
     */
    private void handlePredicateMethod(Predicate predicate, Method method) {
        //
        // One or more method names
        //
        String[] names = predicate.value();
        if (names.length == 0) {
            throw new InternalError(String.format("%s: At least one name required", describe(method)));
        }
        // This simplifies the lambda conversion logic. Instance is possible, but needs special handling
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new InternalError(String.format("%s: Methods must be static", describe(method, names)));
        }
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length == 0) {
            throw new InternalError(String.format("%s: Parameters are required", describe(method, names)));
        }
        // Disambiguate compile-style vs execute-style method
        if (isParamType(paramTypes[0], CompileContext.class)) {
            // Compile style is method(CompileContext,CompoundTerm)
            // While method(CompileContext,Term) is theoretically ok, it is not allowed.
            if (paramTypes.length != 2 ||
                    !isParamType(paramTypes[1], CompoundTerm.class)) {
                throw new InternalError(String.format("%s: Expected %s(CompileContext,CompoundTerm)",
                        describe(method, names), method.getName()));
            }
            // As arity cannot be inferred, it must be specified
            if (predicate.arity() < 0) {
                throw new InternalError(String.format("%s: Arity must be specified", describe(method, names)));
            }
            // Validation done, create a lambda from the method
            handleCompileMethod(names, predicate.arity(), predicate.vararg(), method, predicate.notrace());
        } else if (isParamType(paramTypes[0], Environment.class)) {
            // Execution style is method(Environment,Term,...)
            // method(Environment) is ok here, but a singleton Instruction would be more efficient.
            // Arity is inferred. For performance and maintainance reasons, we map arity into a predicate definition
            // class and lambda interface suitable for that arity, allowing the method to use the parameters directly.
            // Therefore we already know the arity from the parameter count.
            int arity = paramTypes.length - 1;
            for (int i = 1; i < paramTypes.length; i++) {
                if (!isParamType(paramTypes[i], Term.class)) {
                    throw new InternalError(String.format("%s: Expected %s(Environment,Term,...), but found %s at position %d",
                            describe(method, names), method.getName(), paramTypes[i].getName(), i + 1));
                }
            }
            // If arity was specified, make sure they match
            if (predicate.arity() >= 0 && predicate.arity() != arity) {
                throw new InternalError(String.format("%s: Lambda mismatch", describe(method, names)));
            }
            if (predicate.vararg()) {
                throw new InternalError(String.format("%s: Vararg not supported", describe(method, names)));
            }
            // Validation done, create a lambda from the method
            handleExecutionMethod(names, arity, method, predicate.notrace());
        }
    }

    /**
     * Create predicates from method when the method is used to compile.
     *
     * @param names   Names of predicates
     * @param arity   Arity of predicate
     * @param method  Reflected method
     * @param notrace True if tracing disabled
     */
    private void handleCompileMethod(String[] names, int arity, boolean vararg, Method method, boolean notrace) {
        // One definition can be shared by all predicates.
        BuiltInPredicate defn =
                new BuiltinPredicateCompiles(
                        createLambda(BuiltinPredicateCompiles.Compiles.class,
                                BuiltinPredicateCompiles.METHOD_NAME,
                                method)
                );
        for (String name : names) {
            Builtins.define(Builtins.predicate(name, arity), defn, notrace);
        }
        if (vararg) {
            for (String name : names) {
                Builtins.defineVarArg(Builtins.predicate(name, arity), defn);
            }
        }
    }

    /**
     * Case when method is used to execute. Compilation simply compiles the instruction wrapping
     * this method.
     *
     * @param names   Names of predicates
     * @param arity   Arity of predicate
     * @param method  Reflected method
     * @param notrace True if tracing disabled
     */
    private void handleExecutionMethod(String[] names, int arity, Method method, boolean notrace) {
        if (arity >= byArity.length) {
            throw new InternalError("Arity larger than supported, add a new table entry");
        }
        Entry entry = byArity[arity]; // If array out of bounds occurs, add new entries to array

        // Create a lambda function for execution, suitable for given arity
        Object executionLambda = createLambda(
                entry.lambdaClass,
                Entry.METHOD_NAME,
                method);

        // Create containing predicate object suitable for given arity
        BuiltInPredicate defn;
        try {
            Constructor<? extends BuiltInPredicate> cons = entry.predicateClass.getConstructor(entry.lambdaClass);
            defn = cons.newInstance(executionLambda);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new InternalError(e.getMessage(), e);
        }
        // One definition can be shared by all predicates.
        for (String name : names) {
            Builtins.define(Builtins.predicate(name, arity), defn, notrace);
        }
    }

    /**
     * Common functionality to validate names/arity and retrieve singleton field.
     *
     * @param arity Arity of predicate (if predicate names), else 0.
     * @param field Reflected field
     * @param names Array of predicate names, or single resource name.
     * @return singleton
     */
    private static Object getSingleton(int arity, Field field, String... names) {
        if (names.length == 0) {
            // only applicable for predicate names
            throw new InternalError(String.format("%s: At least one name required", describe(field)));
        }
        if (arity < 0) {
            throw new InternalError(String.format("%s: Arity must be specified >= 0", describe(field, names)));
        }
        // Expect a static
        Object value;
        if (!Modifier.isStatic(field.getModifiers())) {
            throw new InternalError(String.format("%s: Field must be static", describe(field)));
        }
        try {
            value = field.get(null); // static get
        } catch (IllegalAccessException e) {
            throw new InternalError(String.format("%s: %s", describe(field), e.getMessage()), e);
        }
        return value;
    }

    /**
     * Predicate described by singleton Instruction field
     *
     * @param predicate Annotation
     * @param field     Reflective Field
     */
    private void handlePredicateField(Predicate predicate, Field field) {
        String[] names = predicate.value();
        int arity = predicate.arity();
        if (arity > 0) {
            throw new InternalError(String.format("%s: Arity > 0 not permitted", describe(field, names)));
        }
        if (predicate.vararg()) {
            throw new InternalError(String.format("%s: Vararg not permitted", describe(field, names)));
        }
        arity = 0; // ignore negative arity
        Object value = getSingleton(arity, field, names);
        if (!(value instanceof Instruction)) {
            throw new InternalError(String.format("%s: Expected to be an Instruction", describe(field, names)));
        }
        Instruction singleton = (Instruction) value;
        // Same singleton can be used for all predicates
        for (String name : names) {
            Builtins.define(Builtins.predicate(name, arity), singleton, predicate.notrace());
        }
    }

    /**
     * CompareImpl Predicate described by singleton field
     *
     * @param compare Annotation
     * @param field   Reflective Field
     */
    private void handleComparePredicateField(Compare compare, Field field) {
        String[] names = compare.value();
        Object value = getSingleton(2, field, names);
        if (!(value instanceof StackFunction)) {
            throw new InternalError(String.format("%s: Expected to be a StackFunction", describe(field, names)));
        }
        StackFunction singleton = (StackFunction) value;
        // Same singleton can be used for all predicates
        for (String name : names) {
            Builtins.defineCompare(Builtins.predicate(name, 2), singleton);
        }
    }

    /**
     * Arithmatic function described by singleton field
     *
     * @param function Annotation
     * @param field    Reflective Field
     */
    private void handleFunctionField(Function function, Field field) {
        String[] names = function.value();
        int arity = function.arity();
        Object value = getSingleton(arity, field, names);
        if (!(value instanceof StackFunction)) {
            throw new InternalError(String.format("%s: Expected to be a StackFunction", describe(field, names)));
        }
        StackFunction singleton = (StackFunction) value;
        // All functions can share the same singleton
        for (String name : names) {
            Builtins.defineFunction(Builtins.predicate(name, arity), singleton);
        }
    }

    /**
     * Defers loading of a resource until one of the specified predicates is called. In this case the annotation
     * describes a resource name containing the Prolog script. The Prolog may contain comments, clauses, facts,
     * and ':-' directives. Such directives are expected to succeed. Backtracking is considered an internal error.
     *
     * @param demandLoad Information about resource
     * @param field      Field containing remaining data
     */
    private void handleDemandLoadField(DemandLoad demandLoad, Field field) {
        String resourceName = demandLoad.value();
        Object value = getSingleton(0, field, resourceName);
        if (!(value instanceof Predication[])) {
            throw new InternalError(String.format("%s: Expected to be a Predication[] array", resourceName));
        }
        // The same loader may be used for each predicate.
        LoadResourceOnDemand onDemand = new LoadResourceOnDemand(getClass(), resourceName);
        for (Predication p : ((Predication[]) value)) {
            Builtins.onDemand(new Predication.Interned((PrologAtomInterned) p.functor(), p.arity()), onDemand);
        }
    }

    /**
     * Creates a Lambda proxy to invoke static method.
     *
     * @param lambdaClass      Class of lambda interface.
     * @param lambdaMethodName Name of method implemented in lambdaClass.
     * @param method           Reflected method of static method to call
     * @param <T>              Type of interface (i.e. same as lambdaClass).
     * @return callable interface
     */
    @SuppressWarnings("unchecked")
    private <T> T createLambda(Class<T> lambdaClass, String lambdaMethodName, Method method) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.unreflect(method);
            MethodType func = handle.type();
            CallSite site = LambdaMetafactory.metafactory(
                    lookup,
                    lambdaMethodName, // must match interface
                    MethodType.methodType(lambdaClass),
                    func,
                    handle,
                    func);
            return (T) site.getTarget().invoke();
        } catch (Throwable e) {
            throw new InternalError(e.getMessage(), e);
        }
    }

    /**
     * Helper class to map arity to definition classes and Lambda interfaces.
     */
    private static class Entry {
        static final String METHOD_NAME = "call"; // method exposed by lambdaClass
        final Class<? extends BuiltInPredicate> predicateClass;
        final Class<?> lambdaClass;

        Entry(Class<? extends BuiltInPredicate> predicateClass, Class<?> ifaceClass) {
            this.predicateClass = predicateClass;
            this.lambdaClass = ifaceClass;
        }
    }
}
