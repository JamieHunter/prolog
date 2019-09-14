// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.parser;

import org.jprolog.expressions.Term;

/**
 * All active parsing state subclass from this.
 */
/*package*/
abstract class ActiveParsingState implements ParseState {
    final Tokenizer tokenizer;

    /**
     * Create a new active parsing state
     *
     * @param tokenizer Associated tokenizer
     */
    ActiveParsingState(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    /**
     * @return false - still parsing.
     */
    @Override
    public boolean done() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term term() {
        throw new InternalError("Unexpected call");
    }
}
