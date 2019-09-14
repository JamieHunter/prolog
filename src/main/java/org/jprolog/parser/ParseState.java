// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.parser;

import org.jprolog.expressions.Term;

import java.io.IOException;

/**
 * Parse state interface. State is either an instance of {@link ActiveParsingState} or {@link TokenFinishedState}.
 */
/*package*/
interface ParseState {

    /**
     * Called to indicate parsing has completed.
     *
     * @param term token Output token
     * @return TokenFinishedState state
     */
    static TokenFinishedState finish(Term term) {
        return new TokenFinishedState(term);
    }

    /**
     * Continue parsing, specifying next state
     *
     * @return Next state
     * @throws IOException on IO error.
     */
    ParseState next() throws IOException;

    /**
     * @return true if done (term exists)
     */
    boolean done();

    /**
     * @return Term if final term.
     */
    Term term();
}
