// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.parser;

import prolog.exceptions.PrologSyntaxError;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Class to handle parsing a block comment.
 */
/*package*/
class BlockCommentState extends ActiveParsingState {

    private final LineMatcher lineMatcher;
    private static final Pattern PATTERN = Pattern.compile("\\*\\/");

    /**
     * {@inheritDoc}
     */
    BlockCommentState(Tokenizer tokenizer, LineMatcher lineMatcher) {
        super(tokenizer);
        this.lineMatcher = lineMatcher.split(PATTERN);
    }

    /**
     * {@inheritDoc}
     */
    public ParseState next() throws IOException {
        while (lineMatcher.find() < 0) {
            if (!tokenizer.newLine(lineMatcher)) {
                throw PrologSyntaxError.commentError(tokenizer.environment(), "End of file reached before finding '*/'");
            }
        }
        lineMatcher.end();
        return tokenizer.parseAnyToken();
    }
}
