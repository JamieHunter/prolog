// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.exceptions;

import prolog.bootstrap.Interned;
import prolog.constants.PrologAtomInterned;
import prolog.execution.Environment;
import prolog.expressions.Term;

/**
 * Prolog Existence error. An existence error occurs when type and domain are valid, but the resource does not exist.
 */
public class PrologExistenceError extends PrologError {

    /**
     * Create new existence error
     *
     * @param environment Execution environment
     * @param type        Kind of existence error as an atom
     * @param message     Display message
     * @param cause       Java cause if any, else null
     * @return exception (not thrown)
     */
    public static PrologExistenceError error(Environment environment, PrologAtomInterned type, Term target, String message, Throwable cause) {
        return new PrologExistenceError(
                formal(Interned.EXISTENCE_ERROR_FUNCTOR, type, target),
                context(environment, message),
                cause);
    }

    /**
     * {@inheritDoc}
     */
    protected PrologExistenceError(Term formal, ErrorContext context, Throwable cause) {
        super(formal, context, cause);
    }

}
