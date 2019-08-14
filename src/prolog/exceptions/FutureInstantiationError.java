// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.exceptions;

import prolog.constants.PrologAtomInterned;
import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.variables.Variable;

/**
 * This will get turned into a PrologInstantiationError once context is known
 */
public class FutureInstantiationError extends FuturePrologError {
    private final Term term;

    private static String toMessage(Term target) {
        String message = "Variable is not instantiated";
        if (target instanceof Variable) {
            message = "Variable '" + ((Variable) target).name() + "' is not instantiated";
        }
        return message;
    }

    /**
     * Construct a FutureInstantiationError
     *
     * @param term Term that caused error.
     */
    public FutureInstantiationError(Term term) {
        super(toMessage(term));
        this.term = term;
    }

    /**
     * @return term in error
     */
    public Term getTerm() {
        return term;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologInstantiationError toError(Environment environment) {
        return PrologInstantiationError.error(environment, this);
    }
}
