// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.exceptions;

import prolog.bootstrap.Interned;
import prolog.constants.PrologAtomInterned;
import prolog.execution.Environment;
import prolog.expressions.Term;

/**
 * Prolog Evaluation error. An evaluation error occurs for example, when a division by zero occurs.
 */
public class PrologEvaluationError extends PrologError {

    /**
     * Evaluation error
     *
     * @param environment Execution environment
     * @param type        Type of error as an atom
     * @param message     Display message
     * @param cause       Underlying Java cause if any, or null if not
     * @return exception
     */
    public static PrologEvaluationError error(Environment environment, PrologAtomInterned type, String message, Throwable cause) {
        return new PrologEvaluationError(
                formal(Interned.EVALUATION_ERROR_FUNCTOR, type),
                context(environment, message),
                cause);
    }

    /**
     * Completion of {@link FutureEvaluationError}.
     *
     * @param environment Execution environment
     * @param cause       Java exception
     * @return Evaluation error (not thrown)
     */
    public static PrologEvaluationError error(Environment environment, FutureEvaluationError cause) {
        return error(environment, cause.getType(), cause.getMessage(), cause);
    }

    /**
     * {@inheritDoc}
     */
    protected PrologEvaluationError(Term formal, ErrorContext context, Throwable cause) {
        super(formal, context, cause);
    }

}
