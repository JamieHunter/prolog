// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.unification;

import prolog.execution.LocalContext;
import prolog.expressions.Term;

/**
 * When unifying, one side is compiled into a unifier, and then applied to the other side.
 */
public interface Unifier {

    /**
     * Unify two terms. Terms are assumed to be pre-bound and simplified.
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
     * Unify the pre-compiled term with another term.
     *
     * @param context Local context for variable bindings.
     * @param other   Other term to unify with.
     * @return true if success.
     */
    boolean unify(LocalContext context, Term other);
}
