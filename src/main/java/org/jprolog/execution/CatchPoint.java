// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.execution;

import org.jprolog.exceptions.PrologThrowable;
import org.jprolog.expressions.Term;

/**
 * Catch points are visited when an exception is thrown until the TERMINAL catch point is handled.
 */
public class CatchPoint {

    /**
     * Special catch point that is at the bottom of the chain of catches.
     */
    public static final CatchPoint TERMINAL = new CatchPoint();

    /**
     * Handle the thrown term. Term is assumed "resolved", that is, grounded to the extent possible.
     *
     * @param thrown Thrown term
     * @return true if handled
     */
    public boolean tryCatch(Term thrown) {
        if (thrown instanceof RuntimeException) {
            throw (RuntimeException) thrown;
        }
        throw new PrologThrowable(thrown, "Prolog uncaught throw", null);
    }
}
