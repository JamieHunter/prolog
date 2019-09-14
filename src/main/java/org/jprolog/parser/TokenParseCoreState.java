// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.parser;

import org.jprolog.constants.PrologAtom;
import org.jprolog.exceptions.PrologSyntaxError;

import java.io.IOException;

/**
 * This state will parse a number of tokens directly. More complex tokens are delegated to dedicated parsers.
 */
/*package private*/
class TokenParseCoreState extends ActiveParsingState {

    private final LineMatcher lineMatcher;

    TokenParseCoreState(Tokenizer tokenizer, LineMatcher lineMatcher) {
        super(tokenizer);
        this.lineMatcher = lineMatcher;
    }

    /**
     * Match with type of token per regex. Either finishing or entering a new state.
     *
     * @return Next state
     * @throws IOException on IO error
     */
    @Override
    public ParseState next() throws IOException {
        if (!lineMatcher.scanNext()) {
            throw PrologSyntaxError.tokenError(tokenizer.environment(), "Failed to parse: " + tokenizer.errorLine());
        }
        String match;
        match = lineMatcher.group(Tokenizer.WS_TAG);
        if (match != null) {
            // trivial whitespace is ignored
            return this;
        }
        match = lineMatcher.group(Tokenizer.ATOM_TAG);
        if (match != null) {
            // Simple atom is accepted as is (at this point, not interned)
            return ParseState.finish(new PrologAtom(match));
        }
        match = lineMatcher.group(Tokenizer.FLOAT_TAG);
        if (match != null) {
            // Simple float is decoded and returned
            return ParseState.finish(tokenizer.parseFloat(match));
        }
        match = lineMatcher.group(Tokenizer.INTEGER_TAG);
        if (match != null) {
            // Simple integer is decoded and returned
            return ParseState.finish(tokenizer.parseInteger(match));
        }
        match = lineMatcher.group(Tokenizer.VARIABLE_TAG);
        if (match != null) {
            // Variables are given identifiers per name
            return ParseState.finish(tokenizer.parseVariable(match));
        }
        match = lineMatcher.group(Tokenizer.ANON_VARIABLE_TAG);
        if (match != null) {
            // Anonymous variables are all given unique identifiers
            return ParseState.finish(tokenizer.parseAnonymousVariable(match));
        }
        match = lineMatcher.group(Tokenizer.START_CODE_TAG);
        if (match != null) {
            // Special parsing for character code
            return QuotedContextState.newCharCode(tokenizer, lineMatcher);
        }
        match = lineMatcher.group(Tokenizer.START_STRING_TAG);
        if (match != null) {
            // Special parsing for quoted strings, quoted atoms etc.
            return QuotedContextState.newQuotedText(tokenizer, lineMatcher, match);
        }
        match = lineMatcher.group(Tokenizer.START_LINE_COMMENT_TAG);
        if (match != null) {
            // Special parsing for line comments
            return new LineCommentState(tokenizer, lineMatcher);
        }
        match = lineMatcher.group(Tokenizer.START_BLOCK_COMMENT_TAG);
        if (match != null) {
            // Special parsing for block comments
            return new BlockCommentState(tokenizer, lineMatcher);
        }
        match = lineMatcher.group(Tokenizer.CATCH_ALL_TAG);
        if (match != null) {
            if (match.length() == 0) {
                if (tokenizer.newLine(lineMatcher)) {
                    return this;
                } else {
                    return tokenizer.parseReachedEOF();
                }
            }
        }
        throw PrologSyntaxError.tokenError(tokenizer.environment(), "Unhandled lexical: " + tokenizer.errorLine());
    }

}
