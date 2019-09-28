// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.unification;

import org.jprolog.constants.PrologInteger;
import org.jprolog.exceptions.PrologTypeError;
import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.Strings;
import org.jprolog.expressions.Term;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.function.Supplier;

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
    static boolean unify(LocalContext context, Term left, Term right) {
        Unifier unifier = UnifyBuilder.from(left);
        return unifier.unify(context, right);
    }

    /**
     * Non-generic unification of integer
     * @param environment Environment to unify
     * @param parameter Parameter of target value
     * @param value New value
     * @return true if unified, false if backtracking
     */
    static boolean unifyInteger(Environment environment, Term parameter, BigInteger value) {
        if (parameter.isInstantiated()) {
            if (!parameter.isInteger()) {
                throw PrologTypeError.integerExpected(environment, parameter);
            }
            if (((PrologInteger)parameter).get().equals(value)) {
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
     * Unification of any string
     * @param environment Environment to unify
     * @param parameter Parameter of target value
     * @param extract Function to extract string value
     * @param value Supplier of string value
     * @return true if unified, false if backtracking
     */
    static boolean unifyString(Environment environment, Term parameter, String value,
                               Function<Term,String> extract, Function<String,Term> allocate) {
        if (parameter.isInstantiated()) {
            String oldValue = extract.apply(parameter);
            if (oldValue.equals(value)) {
                return true;
            }
        } else {
            if (parameter.instantiate(allocate.apply(value))) {
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
