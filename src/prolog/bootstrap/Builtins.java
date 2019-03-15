// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.bootstrap;

import prolog.constants.PrologAtom;
import prolog.execution.Instruction;
import prolog.functions.StackFunction;
import prolog.predicates.BuiltinPredicateCompare;
import prolog.predicates.BuiltinPredicateSingleton;
import prolog.predicates.ClauseSearchPredicate;
import prolog.predicates.DemandLoadPredicate;
import prolog.predicates.PredicateDefinition;
import prolog.predicates.Predication;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Global map of built in predicates and functions that's used to bootstrap each environment. Collection of these
 * predicates and functions only needs to be done once, and occurs in response to Java's on-demand execution of
 * static initializer. The initializer specifies one or more library classes
 * (see {@link prolog.library.Library} as an example) which when instantiated, adds predicates, functions and atoms
 * to the global tables.
 */
public class Builtins {

    // Must be initialized before calling consult()
    private static final HashMap<Predication, PredicateDefinition> builtins = new HashMap<>();
    private static final HashMap<Predication, StackFunction> functions = new HashMap<>();

    //
    // Add libraries to this list
    //
    static {
        // Begin bootstrap of above maps
        consult(prolog.library.Library.class);
        consult(prolog.flags.FlagSets.class);
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
     */
    public static <T extends PredicateDefinition> T define(Predication predication, T definition) {
        builtins.put(predication, definition);
        return definition;
    }

    /**
     * Create an on-demand predicate. It is a form of {@link ClauseSearchPredicate} that is loaded from
     * a resource.
     *
     * @param predication Name/arity
     * @param onDemand    On-demand handler
     */
    public static DemandLoadPredicate onDemand(Predication predication, LoadResourceOnDemand onDemand) {
        DemandLoadPredicate definition = new DemandLoadPredicate(onDemand);
        return define(predication, definition);
    }

    /**
     * Create a built in predicate for a boolean comparison of two numeric expressions.
     *
     * @param predication Name/arity. Arity is expected to be 2.
     * @param compare     Operation
     */
    public static void defineCompare(Predication predication, StackFunction compare) {
        define(predication, new BuiltinPredicateCompare(compare));
    }

    /**
     * Create a builtin predicate with simple singleton instruction
     *
     * @param predication Name/arity. Arity is expected to be 0.
     * @param instruction Singleton run-time implementation of predicate
     */
    public static BuiltinPredicateSingleton define(Predication predication, Instruction instruction) {
        return define(predication, new BuiltinPredicateSingleton(instruction));
    }

    /**
     * Create a function and add to functions table.
     *
     * @param predication Name/arity
     * @param function    Proxy that operates on the stack
     */
    public static StackFunction defineFunction(Predication predication, StackFunction function) {
        functions.put(predication, function);
        return function;
    }

    /**
     * Called from Environment, retrieves a copy of builtin predicates.
     *
     * @return Builtin predicates
     */
    public static Map<? extends Predication, ? extends PredicateDefinition> getPredicates() {
        return Collections.unmodifiableMap(builtins);
    }

    /**
     * Called from Environment, retrieves a copy of builtin functions.
     *
     * @return Builtin functions
     */
    public static Map<? extends Predication, ? extends StackFunction> getFunctions() {
        return Collections.unmodifiableMap(functions);
    }

    /**
     * Utility method to create a predication for built-in predicates, i.e. the atom is interned.
     *
     * @param name  Name of predicate
     * @param arity Arity of predicate
     * @return Predication
     */
    public static Predication predicate(String name, int arity) {
        PrologAtom functor = Interned.internAtom(name);
        return new Predication(functor, arity);
    }

}
