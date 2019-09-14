// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.exceptions;

import org.jprolog.expressions.Term;
import org.jprolog.bootstrap.Interned;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.execution.Environment;

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
    public static PrologExistenceError error(Environment environment, PrologAtomLike type, Term target, String message, Throwable cause) {
        return new PrologExistenceError(
                formal(Interned.EXISTENCE_ERROR_FUNCTOR, type, target),
                context(environment, message),
                cause);
    }

    /**
     * Stream specifier is invalid (alias not allowed)
     *
     * @param environment Execution environment.
     * @param target      Term with error
     * @return Domain error (not thrown)
     */
    public static PrologExistenceError stream(Environment environment, Term target) {
        return error(environment, Interned.STREAM_TYPE, target,
                String.format("Stream %s not found", target), null);
    }

    /**
     * {@inheritDoc}
     */
    protected PrologExistenceError(Term formal, ErrorContext context, Throwable cause) {
        super(formal, context, cause);
    }

}
