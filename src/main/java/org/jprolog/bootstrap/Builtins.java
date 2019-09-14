// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.bootstrap;

import org.jprolog.constants.PrologAtomInterned;
import org.jprolog.debugging.SpySpec;
import org.jprolog.execution.Instruction;
import org.jprolog.flags.FlagSets;
import org.jprolog.functions.StackFunction;
import org.jprolog.library.Library;
import org.jprolog.predicates.BuiltinPredicateCompare;
import org.jprolog.predicates.BuiltinPredicateSingleton;
import org.jprolog.predicates.ClauseSearchPredicate;
import org.jprolog.predicates.DemandLoadPredicate;
import org.jprolog.predicates.PredicateDefinition;
import org.jprolog.predicates.Predication;
import org.jprolog.predicates.VarArgDefinition;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Global map of built in predicates and functions that's used to bootstrap each environment. Collection of these
 * predicates and functions only needs to be done once, and occurs in response to Java's on-demand execution of
 * static initializer. The initializer specifies one or more library classes
 * (see {@link Library} as an example) which when instantiated, adds predicates, functions and atoms
 * to the global tables.
 */
public class Builtins {

    // Must be initialized before calling consult()
    private static final HashMap<Predication.Interned, PredicateDefinition> builtins = new HashMap<>();
    private static final HashMap<PrologAtomInterned, VarArgDefinition> builtinVarArgs = new HashMap<>();
    private static final HashMap<Predication.Interned, StackFunction> functions = new HashMap<>();
    private static final HashSet<SpySpec> notraceSet = new HashSet<>();

    //
    // Add libraries to this list
    //
    static {
        // Begin bootstrap of above maps
        consult(Library.class);
        consult(FlagSets.class);
    }

    private Builtins() {
        // Utility
    }

    //
    // ===================================================================
    //

    /**
     * A library is consulted by instantiating an instance and calling consult member method on it.
     *
     * @param cls Library Class to instantiate.
     */
    private static void consult(Class<? extends LibraryBase> cls) {
        LibraryBase inst;
        try {
            inst = cls.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new InternalError(e.getMessage(), e);
        }
        inst.consult(cls);
    }

    /**
     * Create a global predicate from a predication.
     *
     * @param predication Name/arity of predicate
     * @param definition  Definition of predicate
     * @param notrace     true if invisible to trace
     */
    public static <T extends PredicateDefinition> T define(Predication.Interned predication, T definition, boolean notrace) {
        builtins.put(predication, definition);
        if (notrace) {
            notrace(predication);
        }
        return definition;
    }

    /**
     * Create a global var-arg predicate from a predication.
     *
     * @param predication Name/arity of predicate
     * @param definition  Definition of predicate
     */
    public static <T extends PredicateDefinition> T defineVarArg(Predication.Interned predication, T definition) {
        builtinVarArgs.put(predication.functor(), new VarArgDefinition(predication, definition));
        return definition;
    }

    /**
     * Create an on-demand predicate. It is a form of {@link ClauseSearchPredicate} that is loaded from
     * a resource.
     *
     * @param predication Name/arity
     * @param onDemand    On-demand handler
     */
    public static DemandLoadPredicate onDemand(Predication.Interned predication, LoadResourceOnDemand onDemand) {
        DemandLoadPredicate definition = new DemandLoadPredicate(onDemand);
        return define(predication, definition, false);
    }

    /**
     * Create a built in predicate for a boolean comparison of two numeric expressions.
     *
     * @param predication Name/arity. Arity is expected to be 2.
     * @param compare     Operation
     */
    public static void defineCompare(Predication.Interned predication, StackFunction compare) {
        define(predication, new BuiltinPredicateCompare(compare), false);
    }

    /**
     * Create a builtin predicate with simple singleton instruction
     *
     * @param predication Name/arity. Arity is expected to be 0.
     * @param instruction Singleton run-time implementation of predicate
     * @param notrace     true if invisible to trace
     */
    public static BuiltinPredicateSingleton define(Predication.Interned predication, Instruction instruction, boolean notrace) {
        BuiltinPredicateSingleton def = define(predication, new BuiltinPredicateSingleton(instruction), notrace);
        return def;
    }

    /**
     * Add builtin predicate as no-trace
     *
     * @param predication Name/arity. Arity is expected to be 0.
     */
    public static void notrace(Predication.Interned predication) {
        SpySpec spec = SpySpec.from(predication.functor(), predication.arity());
        notraceSet.add(spec);
    }

    /**
     * Create a function and add to functions table.
     *
     * @param predication Name/arity
     * @param function    Proxy that operates on the stack
     */
    public static StackFunction defineFunction(Predication.Interned predication, StackFunction function) {
        functions.put(predication, function);
        return function;
    }

    /**
     * Called from Environment, retrieves a copy of builtin predicates.
     *
     * @return Builtin predicates
     */
    public static Map<Predication.Interned, PredicateDefinition> getPredicates() {
        return Collections.unmodifiableMap(builtins);
    }

    /**
     * Called from Environment, retrieves a copy of builtin predicates.
     *
     * @return Builtin predicates
     */
    public static Map<PrologAtomInterned, VarArgDefinition> getVarArgPredicates() {
        return Collections.unmodifiableMap(builtinVarArgs);
    }

    /**
     * Called from Environment, retrieves a copy of builtin functions.
     *
     * @return Builtin functions
     */
    public static Map<Predication.Interned, StackFunction> getFunctions() {
        return Collections.unmodifiableMap(functions);
    }

    /**
     * Called from ActiveDebugger to determine if a predicate is in the ignore list
     *
     * @param spec Specification to test
     * @return true if trace is disabled
     */
    public static boolean isNoTrace(SpySpec spec) {
        return notraceSet.contains(spec);
    }

    /**
     * Utility method to create a predication for built-in predicates, i.e. the atom is interned.
     *
     * @param name  Name of predicate
     * @param arity Arity of predicate
     * @return Predication
     */
    public static Predication.Interned predicate(String name, int arity) {
        PrologAtomInterned functor = Interned.internAtom(name);
        return new Predication.Interned(functor, arity);
    }

}
