// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.exceptions;

import org.jprolog.expressions.Term;
import org.jprolog.bootstrap.Interned;
import org.jprolog.execution.Environment;

/**
 * Error is thrown in response to abort.
 */
public class PrologAborted extends PrologError {

    /**
     * Return a constructed error to throw.
     *
     * @param environment Execution environment
     * @return PrologAborted (not thrown)
     */
    public static PrologAborted abort(Environment environment) {
        String message = "Execution aborted";
        return new PrologAborted(
                Interned.ABORTED_ATOM,
                context(environment, "Execution aborted"),
                null);
    }

    /**
     * {@inheritDoc}
     */
    protected PrologAborted(Term formal, ErrorContext context, Throwable cause) {
        super(formal, context, cause);
    }
}
