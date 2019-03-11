// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.parser;

import prolog.exceptions.PrologSyntaxError;
import prolog.expressions.Term;

import java.io.IOException;
import java.util.regex.Matcher;

/**
 * This state will parse a number of tokens directly. More complex tokens are delegated to dedicated parsers.
 */
/*package private*/
class TokenParseCoreState extends ActiveParsingState {

    private final Matcher matcher;

    TokenParseCoreState(Tokenizer tokenizer, String text) {
        super(tokenizer);
        matcher = Tokenizer.CORE_PATTERN.matcher(text);
    }

    /**
     * Move to next portion of text that has not yet been matched.
     *
     * @return self
     */
    private ParseState proceed() {
        matcher.region(matcher.end(), matcher.regionEnd());
        return this;
    }

    /**
     * Complete token processing and return Term.
     *
     * @param term Term to be returned
     * @return {@link TokenFinishedState}
     * @throws IOException on IO error
     */
    private ParseState finish(Term term) throws IOException {
        tokenizer.consume(matcher);
        return ParseState.finish(term);
    }

    /**
     * Transition to another state after comitting the token so far.
     *
     * @param next Next state
     * @return next state
     * @throws IOException on IO error
     */
    private ParseState enter(ParseState next) throws IOException {
        tokenizer.consume(matcher);
        return next;
    }

    /**
     * Match with type of token per regex. Either finishing or entering a new state.
     *
     * @return Next state
     * @throws IOException on IO error
     */
    @Override
    public ParseState next() throws IOException {
        if (!matcher.lookingAt()) {
            throw PrologSyntaxError.tokenError(tokenizer.environment(), "Failed to parse: " + tokenizer.readLine());
        }
        String match;
        match = matcher.group(Tokenizer.WS_TAG);
        if (match != null) {
            // trivial whitespace is ignored
            return proceed();
        }
        match = matcher.group(Tokenizer.ATOM_TAG);
        if (match != null) {
            // Simple atom is accepted as is
            return finish(tokenizer.environment().getAtom(match));
        }
        match = matcher.group(Tokenizer.FLOAT_TAG);
        if (match != null) {
            // Simple float is decoded and returned
            return finish(tokenizer.parseFloat(match));
        }
        match = matcher.group(Tokenizer.INTEGER_TAG);
        if (match != null) {
            // Simple integer is decoded and returned
            return finish(tokenizer.parseInteger(match));
        }
        match = matcher.group(Tokenizer.VARIABLE_TAG);
        if (match != null) {
            // Variables are given identifiers per name
            return finish(tokenizer.parseVariable(match));
        }
        match = matcher.group(Tokenizer.ANON_VARIABLE_TAG);
        if (match != null) {
            // Anonymous variables are all given unique identifiers
            return finish(tokenizer.parseAnonymousVariable(match));
        }
        match = matcher.group(Tokenizer.START_CODE_TAG);
        if (match != null) {
            // Special parsing for character code
            return enter(QuotedContextState.newCharCode(tokenizer));
        }
        match = matcher.group(Tokenizer.START_STRING_TAG);
        if (match != null) {
            // Special parsing for quoted strings, quoted atoms etc.
            return enter(QuotedContextState.newQuotedText(tokenizer, match));
        }
        match = matcher.group(Tokenizer.START_LINE_COMMENT_TAG);
        if (match != null) {
            // Special parsing for line comments
            return enter(new LineCommentState(tokenizer));
        }
        match = matcher.group(Tokenizer.START_BLOCK_COMMENT_TAG);
        if (match != null) {
            // Special parsing for block comments
            return enter(new BlockCommentState(tokenizer));
        }
        match = matcher.group(Tokenizer.CATCH_ALL_TAG);
        if (match != null) {
            if (match.length() == 0) {
                // assume finish of line
                tokenizer.mark();
                return tokenizer.readLineAndParseToken();
            }
        }
        tokenizer.consume(matcher);
        throw new InternalError("Unhandled lexical: " + tokenizer.readLine());
    }

}
