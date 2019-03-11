// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.exceptions;

import prolog.bootstrap.Interned;
import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.variables.Variable;

/**
 * Error is thrown if a variable is not instantiated but a value is required.
 */
public class PrologInstantiationError extends PrologError {

    /**
     * Given an execution environment and term (assumed to be a variable), return
     * a constructed error.
     *
     * @param environment Execution environment
     * @param target      Term assumed to be an uninstantiated variable
     * @return PrologInstantiationError (not thrown)
     */
    public static PrologInstantiationError error(Environment environment, Term target) {
        String message = "Variable is not instantiated";
        if (target instanceof Variable) {
            message = "Variable '" + ((Variable) target).name() + "' is not instantiated";
        }
        return new PrologInstantiationError(
                Interned.INSTANTIATION_ERROR_ATOM,
                context(environment, message),
                null);
    }

    /**
     * {@inheritDoc}
     */
    protected PrologInstantiationError(Term formal, ErrorContext context, Throwable cause) {
        super(formal, context, cause);
    }

}
