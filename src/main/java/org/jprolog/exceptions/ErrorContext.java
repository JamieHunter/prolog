// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.exceptions;

import org.jprolog.expressions.CompoundTermImpl;
import org.jprolog.predicates.Predication;
import org.jprolog.bootstrap.Interned;
import org.jprolog.constants.PrologString;

/**
 * Deferred error context. It captures the predication and the error message. The context is "system defined" per
 * ISO standard.
 */
public class ErrorContext extends CompoundTermImpl {

    private final String message;

    /**
     * Create a context given a predication and a message
     *
     * @param predication Functor/arity
     * @param message     Text message
     */
    public ErrorContext(Predication predication, String message) {
        super(Interned.CONTEXT_FUNCTOR, predication.term(),
                new PrologString(message));
        this.message = message;
    }

    /**
     * @return error message
     */
    public String getMessage() {
        return message;
    }
}
