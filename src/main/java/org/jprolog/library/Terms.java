// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Predicate;
import org.jprolog.constants.Atomic;
import org.jprolog.constants.PrologInteger;
import org.jprolog.enumerators.CopyTerm;
import org.jprolog.exceptions.PrologInstantiationError;
import org.jprolog.exceptions.PrologTypeError;
import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.CompoundTermImpl;
import org.jprolog.expressions.Term;
import org.jprolog.unification.Unifier;
import org.jprolog.variables.UnboundVariable;

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
     * Success if term is a compound
     *
     * @param environment Execution environment
     * @param term        Term to test
     */
    @Predicate("compound")
    public static void compound(Environment environment, Term term) {
        if (!(term instanceof CompoundTerm)) {
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
        if (!struct.isInstantiated()) {
            throw PrologInstantiationError.error(environment, struct);
        }
        if (!index.isInstantiated()) {
            throw PrologInstantiationError.error(environment, index);
        }
        PrologInteger indexInt = PrologInteger.from(index);
        int i = indexInt.notLessThanZero().toInteger();
        Term value = null;
        int arity = 0;
        if (struct instanceof CompoundTerm) {
            CompoundTerm comp = (CompoundTerm) struct;
            arity = comp.arity();
            if (i > 0 && i <= arity) {
                value = comp.get(i - 1);
            }
        } else {
            throw PrologTypeError.compoundExpected(environment, struct);
        }
        if (value == null || !Unifier.unify(environment.getLocalContext(), arg, value)) {
            environment.backtrack();
        }
    }

    /**
     * unify functor with functor of struct, unify arity with arity of struct.
     *
     * @param environment Execution environment
     * @param term        Term, typically instantiated
     * @param name        Functor, typically uninstantiated
     * @param arity       Arity, typically uninstantiated
     */
    @Predicate("functor")
    public static void functor(Environment environment, Term term, Term name, Term arity) {
        // struct is expected to be sufficiently bound to instantiate functor and args
        // however if not bound, functor and arity must be bound
        LocalContext context = environment.getLocalContext();
        if (!term.isInstantiated()) {
            if (!name.isInstantiated()) {
                throw PrologInstantiationError.error(environment, name);
            }
            if (!arity.isInstantiated()) {
                throw PrologInstantiationError.error(environment, arity);
            }
            if (!name.isAtomic()) {
                throw PrologTypeError.atomicExpected(environment, name);
            }
            int arityInt = PrologInteger.from(arity).notLessThanZero().toArity(environment);
            if (arityInt > 0 && !name.isAtom()) {
                throw PrologTypeError.atomExpected(environment, name);
            }
            Term newStruct;
            if (arityInt == 0) {
                newStruct = name;
            } else {
                // Build a struct from these terms
                Term[] members = new Term[arityInt];
                for (int i = 0; i < arityInt; i++) {
                    members[i] = new UnboundVariable("_", environment.nextVariableId()).resolve(context);
                }
                newStruct = new CompoundTermImpl((Atomic) name, members);
            }
            if (!term.instantiate(newStruct)) {
                throw new UnsupportedOperationException("Unable to instantiate");
            }
            return;
        }
        if (term.isAtomic()) {
            term = CompoundTerm.from((Atomic) term);
        }
        CompoundTerm comp = (CompoundTerm) term;
        if (!(Unifier.unify(context, name, comp.functor()) &&
                Unifier.unify(context, arity, PrologInteger.from(comp.arity()))
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
