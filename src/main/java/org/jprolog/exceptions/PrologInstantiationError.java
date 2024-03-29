// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.exceptions;

import org.jprolog.expressions.Term;
import org.jprolog.bootstrap.Interned;
import org.jprolog.execution.Environment;
import org.jprolog.variables.Variable;

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

    /**
     * Create instantiation error from future error
     *
     * @param environment Execution environment.
     * @return Instantiation error (not thrown)
     */
    public static PrologInstantiationError error(Environment environment, FutureInstantiationError error) {
        return error(environment, error.getTerm());
    }

}
