// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.exceptions;

import org.jprolog.execution.Environment;

/**
 * Base class for partially filled prolog errors. These are thrown at time of relevance when Environment and other
 * context may not be known. It is converted into a subclass of {@link PrologError} prior to handling by Prolog
 * interpreter.
 */
public abstract class FuturePrologError extends RuntimeException {

    /**
     * Error with message
     * @param message Textual description of error
     */
    protected FuturePrologError(String message) {
        super(message);
    }

    /**
     * Error with message and Java cause
     * @param message Textual description of error
     * @param cause Java cause of error
     */
    protected FuturePrologError(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Error with Java cause only.
     * @param cause Java cause of error
     */
    protected FuturePrologError(Throwable cause) {
        super(cause);
    }

    /**
     * Implemented by subclass, convert to appropriate subclass of {@link PrologError}
     * @param environment Execution environment now known.
     * @return Full error
     */
    abstract public PrologError toError(Environment environment);
}
