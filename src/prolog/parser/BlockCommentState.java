// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.parser;

import prolog.exceptions.PrologSyntaxError;

import java.io.IOException;

/**
 * Class to handle parsing a block comment.
 */
/*package*/
class BlockCommentState extends ActiveParsingState {

    /**
     * {@inheritDoc}
     */
    BlockCommentState(Tokenizer tokenizer) {
        super(tokenizer);
    }

    /**
     * {@inheritDoc}
     */
    public ParseState next() throws IOException {
        String line = tokenizer.readLine();
        if (line == null) {
            throw PrologSyntaxError.commentError(tokenizer.environment(), "End of file reached before finding '*/'");
        }
        int off = line.indexOf("*/");
        if (off < 0) {
            tokenizer.mark();
            return this;
        } else {
            tokenizer.consume(off + 2);
            tokenizer.mark();
            // past comment
            return tokenizer.readLineAndParseToken();
        }
    }
}
