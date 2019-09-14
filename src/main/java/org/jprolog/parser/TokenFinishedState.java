// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.parser;

import org.jprolog.expressions.Term;

import java.io.IOException;

/**
 * Final parse state.
 */
/*package*/
class TokenFinishedState implements ParseState {
    private final Term term;

    /**
     * Create finished state for term.
     *
     * @param term Produced term
     */
    /*package*/
    TokenFinishedState(Term term) {
        this.term = term;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term term() {
        return this.term;
    }

    @Override
    public ParseState next() throws IOException {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean done() {
        return true;
    }
}
