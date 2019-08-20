// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.parser;

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
        return tokenizer.parseAnyToken(); // handles EOF
    }
}
