// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.parser;

import org.jprolog.constants.PrologAtom;
import org.jprolog.flags.ReadOptions;

import java.io.IOException;

/**
 * Class to handle parsing a line comment.
 */
class LineCommentState extends ActiveParsingState {

    /**
     * {@inheritDoc}
     */
    LineCommentState(Tokenizer tokenizer, LineMatcher parser) {
        super(tokenizer);
    }

    /**
     * {@inheritDoc}
     */
    public ParseState next() throws IOException {
        tokenizer.newLine(null); // comment to end of line
        if (tokenizer.options().whiteSpace == ReadOptions.WhiteSpace.ATOM_skip) {
            // comments are ignored (also handles EOF)
            return tokenizer.parseAnyToken();
        } else {
            // options can control how comments are parsed
            return ParseState.finish(new PrologAtom("%")); // TODO: capture comment
        }
    }
}
