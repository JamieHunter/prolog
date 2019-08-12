// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Predicate;
import prolog.constants.Atomic;
import prolog.constants.PrologAtom;
import prolog.constants.PrologInteger;
import prolog.execution.CopyTerm;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.expressions.CompoundTerm;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;
import prolog.unification.Unifier;
import prolog.variables.UnboundVariable;

import java.math.BigInteger;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Bootstraps term manipulation predicates.
 */
public class Terms {

    private Terms() {
    }

    /**
     * Success if term is an atom
     *
     * @param environment Execution environment
     * @param term        Term to test
     */
    @Predicate("atom")
    public static void atom(Environment environment, Term term) {
        if (!term.isAtom()) {
            environment.backtrack();
        }
    }

    /**
     * Success if term is atomic
     *
     * @param environment Execution environment
     * @param term        Term to test
     */
    @Predicate("atomic")
    public static void atomic(Environment environment, Term term) {
        if (!term.isAtomic()) {
            environment.backtrack();
        }
    }

    /**
     * Success if term is an integer
     *
     * @param environment Execution environment
     * @param term        Term to test
     */
    @Predicate("integer")
    public static void integer(Environment environment, Term term) {
        if (!term.isInteger()) {
            environment.backtrack();
        }
    }

    /**
     * Success if term is a real number
     *
     * @param environment Execution environment
     * @param term        Term to test
     */
    @Predicate({"float", "real"})
    public static void floating(Environment environment, Term term) {
        if (!term.isFloat()) {
            environment.backtrack();
        }
    }

    /**
     * Success if term is a number
     *
     * @param environment Execution environment
     * @param term        Term to test
     */
    @Predicate("number")
    public static void number(Environment environment, Term term) {
        if (!(term.isNumber())) {
            environment.backtrack();
        }
    }

    /**
     * Success if term is a string
     *
     * @param environment Execution environment
     * @param term        Term to test
     */
    @Predicate("string")
    public static void string(Environment environment, Term term) {
        if (!(term.isString())) {
            environment.backtrack();
        }
    }

    /**
     * Success if term is an uninstantiated variable
     *
     * @param environment Execution environment
     * @param term        Term to test
     */
    @Predicate("var")
    public static void var(Environment environment, Term term) {
        if (term.isInstantiated()) {
            environment.backtrack();
        }
    }

    /**
     * Success if term is instantiated.
     *
     * @param environment Execution environment
     * @param term        Term to test
     */
    @Predicate("nonvar")
    public static void nonvar(Environment environment, Term term) {
        if (!term.isInstantiated()) {
            environment.backtrack();
        }
    }

    /**
     * Unify arg with the specific indexed argument of structure. Index and Structure are required
     * to be instantiated. Arg is typically uninstantiated.
     *
     * @param environment Execution environment
     * @param index       Index of arg of struct
     * @param struct      Structure (compound term)
     * @param arg         Value of argument, unified with the selected argument of the compound term
     */
    @Predicate("arg")
    public static void arg(Environment environment, Term index, Term struct, Term arg) {
        // index and struct are expected to have values
        if (!(struct instanceof CompoundTerm && index.isInteger())) {
            environment.backtrack();
            return;
        }
        PrologInteger indexInt = PrologInteger.from(index);
        int i = indexInt.get().intValue() - 1;
        CompoundTerm comp = (CompoundTerm) struct;
        if (i < 0 || i >= comp.arity()) {
            throw new IndexOutOfBoundsException("Specified an index out of bounds");
        }
        if (!Unifier.unify(environment.getLocalContext(), arg, comp.get(i))) {
            environment.backtrack();
        }
    }

    /**
     * unify functor with functor of struct, unify arity with arity of struct.
     *
     * @param environment Execution environment
     * @param struct      Structure, sufficiently instantiated
     * @param functor     Functor, typically uninstantiated
     * @param arity       Lambda, typically uninstantiated
     */
    @Predicate("functor")
    public static void functor(Environment environment, Term struct, Term functor, Term arity) {
        // struct is expected to be sufficiently bound to instantiate functor and args
        // however if not bound, functor and arity must be bound
        LocalContext context = environment.getLocalContext();
        if (!struct.isInstantiated()) {
            if (!(functor.isAtom() && arity.isInteger())) {
                environment.backtrack();
                return;
            }
            int arityInt = PrologInteger.from(arity).get().intValue();
            if (arityInt < 0) {
                throw new IndexOutOfBoundsException("Specified an arity out of bounds");
            }
            Atomic functorAtom = PrologAtom.from(functor);
            if (arityInt == 0) {
                struct = functorAtom;
            } else {
                // Build a struct from these terms
                Term[] members = new Term[arityInt];
                for (int i = 0; i < arityInt; i++) {
                    members[i] = new UnboundVariable("_", environment.nextVariableId()).resolve(context);
                }
                struct = new CompoundTermImpl(functorAtom, members);
            }
            if (!struct.instantiate(functorAtom)) {
                throw new UnsupportedOperationException("Unable to instantiate");
            }
            return;
        }
        if (struct.isAtom()) {
            struct = CompoundTerm.from((Atomic) struct);
        }
        CompoundTerm comp = (CompoundTerm) struct;
        if (!(Unifier.unify(context, functor, comp.functor()) &&
                Unifier.unify(context, arity, new PrologInteger(BigInteger.valueOf(comp.arity())))
        )) {
            environment.backtrack();
        }
    }

    /**
     * Copy a source term such that it has a unique set of variables, and unify with target term.
     *
     * @param environment Execution environment
     * @param source      Source term to copy
     * @param target      Target unified with source
     */
    @Predicate("copy_term")
    public static void copyTerm(Environment environment, Term source, Term target) {
        CopyTerm context = new CopyTerm(environment);
        Term copy = source.enumTerm(context);
        Term bound = copy.resolve(environment.getLocalContext());
        if (!Unifier.unify(environment.getLocalContext(), target, bound)) {
            environment.backtrack();
        }
    }
}
