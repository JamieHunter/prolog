// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.parser;

import org.jprolog.constants.PrologAtom;
import org.jprolog.exceptions.PrologSyntaxError;
import org.jprolog.flags.ReadOptions;

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
        if (tokenizer.options().whiteSpace == ReadOptions.WhiteSpace.ATOM_skip) {
            // comments are ignored
            return tokenizer.parseAnyToken();
        } else {
            // options can control how comments are parsed
            return ParseState.finish(new PrologAtom("/**/")); // TODO: capture comment
        }
    }
}
