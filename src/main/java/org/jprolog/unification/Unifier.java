// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.unification;

import org.jprolog.constants.PrologAtomLike;
import org.jprolog.constants.PrologFloat;
import org.jprolog.constants.PrologInteger;
import org.jprolog.constants.PrologNumber;
import org.jprolog.constants.PrologString;
import org.jprolog.exceptions.PrologTypeError;
import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.Strings;
import org.jprolog.expressions.Term;
import org.jprolog.expressions.TermList;
import org.jprolog.expressions.WorkingTermList;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * When unifying, one side is compiled into a unifier, and then applied to the other side.
 */
public interface Unifier {

    /**
     * Unify two terms. Terms are assumed to be sufficiently resolved.
     *
     * @param context Binding localContext for any contained variables
     * @param left    term
     * @param right   term
     * @return true if unified
     */
    static boolean unifyInternal(LocalContext context, Term left, Term right) {
        Unifier unifier = UnifyBuilder.from(left);
        return unifier.unify(context, right);
    }

    /**
     * Non-generic unification of integer
     *
     * @param environment Environment to unify
     * @param parameter   Parameter of target value
     * @param value       New value
     * @return true if unified, false if backtracking
     */
    static boolean unifyInteger(Environment environment, Term parameter, BigInteger value) {
        if (parameter.isInstantiated()) {
            if (PrologInteger.from(parameter).get().equals(value)) {
                return true;
            }
        } else {
            if (parameter.instantiate(PrologInteger.from(value))) {
                return true;
            }
        }
        environment.backtrack();
        return false;
    }

    /**
     * Non-generic unification of integer
     *
     * @param environment Environment to unify
     * @param parameter   Parameter of target value
     * @param value       New value
     * @return true if unified, false if backtracking
     */
    static boolean unifyInteger(Environment environment, Term parameter, long value) {
        return unifyInteger(environment, parameter, BigInteger.valueOf(value));
    }

    /**
     * Unification of a number
     *
     * @param environment Environment to unify
     * @param parameter   Parameter of target value
     * @param value       Number value to unify with/assign
     * @return true if unified, false if backtracking
     */
    static boolean unifyNumber(Environment environment, Term parameter, PrologNumber value) {
        if (parameter.isInstantiated()) {
            if (parameter.compareTo(value) == 0) {
                return true;
            }
            if (!parameter.isNumber()) {
                throw PrologTypeError.numberExpected(environment, parameter);
            }
        } else {
            if (parameter.instantiate(value)) {
                return true;
            }
        }
        environment.backtrack();
        return false;
    }

    /**
     * Unification of a floating point number
     *
     * @param environment Environment to unify
     * @param parameter   Parameter of target value
     * @param value       Number value to unify with/assign
     * @return true if unified, false if backtracking
     */
    static boolean unifyFloat(Environment environment, Term parameter, double value) {
        if (parameter.isInstantiated()) {
            if (PrologFloat.from(parameter).get().equals(value)) {
                return true;
            }
        } else {
            if (parameter.instantiate(PrologFloat.from(value))) {
                return true;
            }
        }
        environment.backtrack();
        return false;
    }

    /**
     * Unification of any string
     *
     * @param environment Environment to unify
     * @param parameter   Parameter of target value
     * @param value       String to unify with
     * @param extract     Function to extract string value
     * @param create      Function to create prolog representation of string
     * @return true if unified, false if backtracking
     */
    static boolean unifyString(Environment environment, Term parameter, String value,
                               Function<Term, String> extract, Function<String, Term> create) {
        if (parameter.isInstantiated()) {
            String oldValue = extract.apply(parameter);
            if (oldValue.equals(value)) {
                return true;
            }
        } else {
            if (parameter.instantiate(create.apply(value))) {
                return true;
            }
        }
        environment.backtrack();
        return false;
    }

    /**
     * Special case of unifyString when dealing with file names.
     *
     * @param environment Environment to unify
     * @param parameter   Parameter of target value
     * @param path        Path to unify with
     * @return true if unified, false if backtracking
     */
    static boolean unifyPath(Environment environment, Term parameter, Path path) {
        return Unifier.unifyString(environment, parameter, path.toString(),
                Strings::stringFromAtomOrAnyString, PrologString::new);
    }

    /**
     * Unification of an atom with type check
     *
     * @param environment Environment to unify
     * @param parameter   Parameter of target value
     * @param value       Atom value to unify with
     * @return true if unified, false if backtracking
     */
    static boolean unifyAtom(Environment environment, Term parameter, PrologAtomLike value) {
        if (parameter.isInstantiated()) {
            if (parameter.compareTo(value) == 0) {
                return true;
            }
            if (!parameter.isAtom()) {
                throw PrologTypeError.atomExpected(environment, parameter);
            }
        } else {
            if (parameter.instantiate(value)) {
                return true;
            }
        }
        environment.backtrack();
        return false;
    }

    /**
     * Unification of an atomic value with type check
     *
     * @param environment Environment to unify
     * @param parameter   Parameter of target value
     * @param value       Atomic value to unify with
     * @return true if unified, false if backtracking
     */
    static boolean unifyAtomic(Environment environment, Term parameter, Term value) {
        if (parameter.isInstantiated()) {
            if (parameter.compareTo(value) == 0) {
                return true;
            }
            if (!parameter.isAtomic()) {
                throw PrologTypeError.atomicExpected(environment, parameter);
            }
        } else {
            if (parameter.instantiate(value)) {
                return true;
            }
        }
        environment.backtrack();
        return false;
    }

    /**
     * Unification of a generic term.
     *
     * @param environment Environment to unify
     * @param parameter   Parameter of target value
     * @param value       Atomic value to unify with
     * @return true if unified, false if backtracking
     */
    static boolean unifyTerm(Environment environment, Term parameter, Term value) {
        if (parameter == value) {
            return true;
        }
        if (parameter.isInstantiated()) {
            if (value.isInstantiated()) {
                if (Unifier.unifyInternal(environment.getLocalContext(), parameter, value)) {
                    return true;
                }
            } else {
                if (value.instantiate(parameter.resolve(environment.getLocalContext()))) {
                    return true;
                }
            }
        } else if (parameter.instantiate(value.resolve(environment.getLocalContext()))) {
            return true;
        }
        environment.backtrack();
        return false;
    }

    /**
     * Unification of two lists of terms
     *
     * @param environment Environment to unify
     * @param parameters  List of parameters
     * @param list        List of values to unify with
     * @return true if unified, false if backtracking
     */
    static boolean unifyLists(Environment environment, WorkingTermList parameters, WorkingTermList list) {
        if (parameters.isConcrete() && list.isConcrete() && parameters.concreteSize() != list.concreteSize()) {
            // concrete lists can be compared for size quickly
            environment.backtrack();
            return false;
        }
        for (int i = 0; ; i++) {
            Term p = parameters.getAt(i);
            Term q = list.getAt(i);
            if (p != null && q != null) {
                if (!unifyTerm(environment, p, q)) {
                    return false;
                }
            } else {
                Term pp = parameters.subList(i).toTerm();
                Term qq = list.subList(i).toTerm();
                return unifyTerm(environment, pp, qq);
            }
        }
    }

    /**
     * Unification of a list of terms
     *
     * @param environment Environment to unify
     * @param parameter   Parameter of target value
     * @param list        List of values to unify with
     * @return true if unified, false if backtracking
     */
    static boolean unifyList(Environment environment, Term parameter, WorkingTermList list) {
        if (parameter == list) {
            return true; // can happen for empty list
        }
        if (parameter.isInstantiated()) {
            WorkingTermList parameters = TermList.compactList(parameter); // partially validates
            return unifyLists(environment, parameters, list);
        } else {
            if (parameter.instantiate(list.toTerm().resolve(environment.getLocalContext()))) {
                return true;
            }
        }
        environment.backtrack();
        return false;
    }

    /**
     * Unify the pre-compiled term with another term.
     *
     * @param context Local context for variable bindings.
     * @param other   Other term to unify with.
     * @return true if success.
     */
    boolean unify(LocalContext context, Term other);
}
