// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.exceptions;

import org.jprolog.constants.PrologAtomLike;
import org.jprolog.execution.Environment;

/**
 * This will get turned into a PrologEvaluationError once context is known
 */
public class FutureEvaluationError extends FuturePrologError {
    private final PrologAtomLike type;

    /**
     * Create an Evaluation exception that is eventually turned into PrologEvaluationError
     *
     * @param type     Type of exception
     * @param original Original exception
     */
    public FutureEvaluationError(PrologAtomLike type, ArithmeticException original) {
        super(original.getMessage(), original);
        this.type = type;
    }

    /**
     * Create an Evaluation exception that is eventually turned into PrologEvaluationError
     *
     * @param type    Type of exception
     * @param message Description of error
     */
    public FutureEvaluationError(PrologAtomLike type, String message) {
        super(message);
        this.type = type;
    }

    /**
     * @return Type of exception
     */
    public PrologAtomLike getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologEvaluationError toError(Environment environment) {
        return PrologEvaluationError.error(environment, this);
    }
}
