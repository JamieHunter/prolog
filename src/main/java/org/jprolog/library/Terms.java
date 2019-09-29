// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Predicate;
import org.jprolog.constants.Atomic;
import org.jprolog.constants.PrologAtomLike;
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
import org.jprolog.variables.LabeledVariable;

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
        PrologInteger indexInt = PrologInteger.from(index);
        if (struct instanceof CompoundTerm) {
            CompoundTerm comp = (CompoundTerm) struct;
            int i = indexInt.notLessThanZero().toInteger(); // 1-based index
            int arity = comp.arity();
            if (i > 0 && i <= arity) {
                Unifier.unifyTerm(environment, arg, comp.get(i - 1));
            } else {
                environment.backtrack();
            }
        } else {
            throw PrologTypeError.compoundExpected(environment, struct);
        }
    }

    /**
     * unify functor with functor of struct, unify arity with arity of struct.
     *
     * @param environment Execution environment
     * @param term        Term, typically instantiated
     * @param nameTerm    Functor, typically uninstantiated
     * @param arityTerm   Arity, typically uninstantiated
     */
    @Predicate("functor")
    public static void functor(Environment environment, Term term, Term nameTerm, Term arityTerm) {
        // struct is expected to be sufficiently grounded to instantiate functor and args
        // however if not instantiated, functor and arity must be instantiated
        LocalContext context = environment.getLocalContext();
        if (term.isInstantiated()) {
            if (term.isAtomic()) {
                Unifier.unifyInteger(environment, arityTerm, 0);
                Unifier.unifyAtomic(environment, nameTerm, term);
            } else {
                CompoundTerm comp = (CompoundTerm) term;
                Unifier.unifyInteger(environment, arityTerm, comp.arity());
                Unifier.unifyAtom(environment, nameTerm, (PrologAtomLike) comp.functor());
            }
        } else {
            if (!(nameTerm.isInstantiated() && arityTerm.isInstantiated())) {
                throw PrologInstantiationError.error(environment, term);
            }
            if (!nameTerm.isAtomic()) {
                // Per ISO, this is true regardless of arityInt for non-atomic names
                throw PrologTypeError.atomicExpected(environment, nameTerm);
            }
            int arityInt = PrologInteger.from(arityTerm).notLessThanZero().toArity(environment);
            Term newStruct;
            if (arityInt == 0) {
                newStruct = nameTerm;
            } else {
                if (!nameTerm.isAtom()) {
                    throw PrologTypeError.atomExpected(environment, nameTerm);
                }
                // Build a struct from these terms
                Term[] members = new Term[arityInt];
                for (int i = 0; i < arityInt; i++) {
                    members[i] = new LabeledVariable("_", environment.nextVariableId()).resolve(context);
                }
                newStruct = new CompoundTermImpl((Atomic) nameTerm, members);
            }
            Unifier.unifyTerm(environment, term, newStruct);
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
        Term copy = source.enumTerm(new CopyTerm(environment)); // valid even if source is uninstantiated
        Unifier.unifyTerm(environment, target, copy);
    }
}
