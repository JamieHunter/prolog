// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.parser;

import org.jprolog.constants.PrologEOF;
import org.jprolog.constants.PrologFloat;
import org.jprolog.constants.PrologInteger;
import org.jprolog.exceptions.PrologError;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.Term;
import org.jprolog.variables.LabeledVariable;
import org.jprolog.flags.ReadOptions;
import org.jprolog.io.Position;
import org.jprolog.io.PrologInputStream;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Follow Prolog token syntax, producing token terms. An original version used ANTLR, however there is enough nuances
 * that ANTLR wasn't quite sufficient (such as ability to switch between syntactical parsing and consuming
 * individual characters). Conversely the Prolog grammer and features makes this a very practical
 * approach. A state machine is used for tokenization split into 4 key states - core parsing, string parsing,
 * line comment parsing and block comment parsing.
 */
public final class Tokenizer extends TokenRegex {

    private final Environment environment;
    private final ReadOptions options;
    private final PrologInputStream inputStream;
    private final Map<String, LabeledVariable> variableMap = new LinkedHashMap<>(); // order preserved
    private final Position startOfLine = new Position();
    private final Position nextLine = new Position();
    private final Position tokenMark = new Position();
    private LineMatcher topLineMatcher = null;
    private int matcherTokenMark = -1;

    // used for core pattern
    static final String WS_TAG = "ws";
    static final String ATOM_TAG = "at";
    static final String FLOAT_TAG = "fl";
    static final String INTEGER_TAG = "in";
    static final String VARIABLE_TAG = "va";
    static final String ANON_VARIABLE_TAG = "an";
    static final String START_STRING_TAG = "st";
    static final String START_CODE_TAG = "co";
    static final String START_LINE_COMMENT_TAG = "li";
    static final String START_BLOCK_COMMENT_TAG = "bl";
    static final String BAD_BACKSLASH_TAG = "bk";
    static final String CATCH_ALL_TAG = "zz";
    // used for strings
    static final String STRING_CHAR_TAG = "ch";
    static final String META_ESCAPE_TAG = "me";
    static final String CONTROL_ESCAPE_TAG = "co";
    static final String QUOTE_TAG = "qq";
    static final String CODE_ESCAPE_TAG = "xx";

    // Main tokenizer pattern matching
    static final Pattern CORE_PATTERN = Pattern.compile(or(
            group(WS_TAG, WSs),
            group(START_LINE_COMMENT_TAG, START_LINE_COMMENT), // check before atom (%)
            group(START_BLOCK_COMMENT_TAG, START_BLOCK_COMMENT), // check before atom (/*)
            group(ATOM_TAG, or(GRAPHIC, SOLO_GRAPHIC, ALPHA_ATOM)), // simple atom
            group(START_CODE_TAG, START_CHAR_CODE), // parse before integer
            group(FLOAT_TAG, FLOAT),
            group(INTEGER_TAG, or(BINARY, OCTAL, HEX, DECIMAL)),
            group(VARIABLE_TAG, or(VARIABLE, UNDERSCORE_VARIABLE)),
            group(ANON_VARIABLE_TAG, ANONYMOUS),
            group(START_STRING_TAG, START_STRING), // mark string parsing
            group(CATCH_ALL_TAG, CATCH_ALL) // catch-all
    ), Pattern.DOTALL);

    // We may parse strings over multiple lines
    static final Pattern SQ_STRING = Pattern.compile(string("'", "[`\"]", false));
    static final Pattern DQ_STRING = Pattern.compile(string("\"", "['`]", false));
    static final Pattern BQ_STRING = Pattern.compile(string("`", "['\"]", false));
    static final Pattern CHAR_PATTERN = Pattern.compile(string("'", "[`\"]", true));

    /**
     * Build a regex for a quoted string.
     *
     * @param quote    Type of quote
     * @param notQuote Other quote characters not included
     * @param single   true if single character
     * @return Regular expression pattern string.
     */
    static String string(String quote, String notQuote, boolean single) {
        return
                or(group(STRING_CHAR_TAG, or(
                        notQuote,
                        STRING_CHAR + (single ? "" : "++"))),
                        group(META_ESCAPE_TAG, META_ESCAPE),
                        group(CONTROL_ESCAPE_TAG, CONTROL_ESCAPE),
                        group(CODE_ESCAPE_TAG, or(HEX_ESCAPE, OCT_ESCAPE)),
                        group(QUOTE_TAG, quote + quote),
                        group(BAD_BACKSLASH_TAG, BACKSLASH_CATCH_ALL),
                        group(CATCH_ALL_TAG, CATCH_ALL) // catch-all
                );
    }

    /**
     * Build tokenizer for environment reading from a prolog stream.
     *
     * @param environment Execution environment.
     * @param options     Options to control tokenization and general parsing.
     * @param inputStream Input substream
     */
    public Tokenizer(Environment environment, ReadOptions options, PrologInputStream inputStream) {
        this.environment = environment;
        this.options = options;
        this.inputStream = inputStream;
    }

    /**
     * @return environment
     */
    public Environment environment() {
        return environment;
    }

    /**
     * @return parsing options
     */
    public ReadOptions options() {
        return options;
    }

    /**
     * Read next token term. Does not handle any meanings of the tokens. EOF is encoded as {@link PrologEOF}.
     *
     * @return term Token as a term or PrologEOF.
     */
    public Term nextToken() {
        try {
            ParseState state;
            if (beginLine()) {
                state = parseAnyToken(); // initial state
            } else {
                state = parseReachedEOF();
            }

            //
            // Run state-machine to parse a token
            while (!state.done()) {
                state = state.next();
            }
            commit();
            return state.term();
        } catch (IOException | RuntimeException e1) {
            try {
                commit();
            } catch (IOException e2) {
                // ignore
            }
            throw PrologError.convert(environment, e1);
        }
    }

    /**
     * Terminal state, reached EOF, returns EOF token
     *
     * @return final state
     */
    ParseState parseReachedEOF() {
        return ParseState.finish(PrologEOF.EOF);
    }

    /**
     * Typical initial state (or state after comments etc). Looking for any token.
     *
     * @return First state
     */
    ParseState parseAnyToken() {
        return new TokenParseCoreState(this, topLineMatcher);
    }

    /**
     * Line that was parsed so far at time error occurs
     *
     * @return string indicating error line
     */
    public String errorLine() {
        if (matcherTokenMark < 0) {
            return "";
        } else {
            return topLineMatcher.toString().substring(matcherTokenMark);
        }
    }

    /**
     * Executed at end of parsing a token, move mark forward.
     *
     * @throws IOException IO Exception
     */
    protected void commit() throws IOException {
        if (matcherTokenMark < 0) {
            return;
        }
        topLineMatcher.next(); // make sure we're past all tokens
        matcherTokenMark = topLineMatcher.at();
        resetToLineStart();
        inputStream.advance(matcherTokenMark);
        inputStream.getPosition(tokenMark);
    }

    /**
     * Move input stream to start of line to reparse-line (or set position)
     *
     * @throws IOException on IO error
     */
    private void resetToLineStart() throws IOException {
        if (!inputStream.seekPosition(startOfLine)) {
            throw new UnsupportedOperationException("NYI - how to handle this?");
        }
    }

    /**
     * Move input stream to end of last token parse
     *
     * @throws IOException on IO error
     */
    void resetToMark() throws IOException {
        if (!inputStream.seekPosition(tokenMark)) {
            throw new UnsupportedOperationException("NYI - how to handle this?");
        }
    }

    /**
     * Read text from mark to finish of line if needed.
     *
     * @return true if text has been read
     * @throws IOException IO Exception
     */
    boolean beginLine() throws IOException {
        if (topLineMatcher == null || topLineMatcher.atEnd()) {
            Function<String, String> translator = TopLineMatcher::noTranslation;
            if (environment().getFlags().charConversion) {
                translator = environment().getCharConverter()::translate;
            }
            topLineMatcher = new TopLineMatcher(CORE_PATTERN, translator);
            return newLine(topLineMatcher);
        } else {
            return true;
        }
    }

    /**
     * Move to the next line. Update LineMatcher with the new line
     * @param current Current LineMatcher being used
     * @return true if line read, false if EOF
     * @throws IOException on IO error
     */
    boolean newLine(LineMatcher current) throws IOException {
        if (current == null) {
            current = topLineMatcher;
        }
        if (matcherTokenMark >= 0) {
            inputStream.seekPosition(nextLine);
        }
        inputStream.getPosition(tokenMark);
        inputStream.getPosition(startOfLine);
        matcherTokenMark = 0;
        String line = inputStream.readLine(); // read ahead of mark
        if (line == null) {
            current.setAtEnd();
            return false;
        } else {
            inputStream.getPosition(nextLine);
            current.newLine(line);
            return true;
        }
    }

    /**
     * Test to see if next character matches the one given
     *
     * @param test Character to test
     * @return true if matching character
     */
    public boolean isNext(char test) {
        try {
            if (topLineMatcher == null) {
                return false;
            }
            inputStream.seekPosition(tokenMark);
            int c = inputStream.read();
            inputStream.seekPosition(tokenMark);
            return c == test;
        } catch (IOException ioe) {
            throw PrologError.systemError(environment, ioe);
        }
    }

    /**
     * Test to see if next character matches one of the white-space characters
     * through until and including end of line
     */
    public void skipEOLN() {
        Position start = new Position();
        try {
            inputStream.getPosition(start);
            for (; ; ) {
                int c = inputStream.read();
                if (c < 0 || c == '\n') {
                    return;
                }
                if (c != ' ' && c != '\r' && c != '\t') {
                    inputStream.seekPosition(start);
                    break;
                }
            }
        } catch (IOException ioe) {
            throw PrologError.systemError(environment, ioe);
        }
    }

    Term parseFloat(String text) {
        text = text.replace("_", "");
        return new PrologFloat(Double.parseDouble(text));
    }

    Term parseInteger(String text) {
        text = text.replace("_", "");
        if (text.startsWith("0x")) {
            return decodeInteger(16, text, 2);
        }
        if (text.startsWith("0o")) {
            return decodeInteger(8, text, 2);
        }
        if (text.startsWith("0b")) {
            return decodeInteger(2, text, 2);
        }
        return decodeInteger(10, text);
    }

    /**
     * Access all variables collected.
     * @return Collection of variables.
     */
    public Map<String, LabeledVariable> getVariableMap() {
        return Collections.unmodifiableMap(variableMap);
    }

    Term parseVariable(String text) {
        return variableMap.computeIfAbsent(text,
                n -> new LabeledVariable(n, environment.nextVariableId()));
    }

    Term parseAnonymousVariable(String text) {
        return new LabeledVariable("_", environment.nextVariableId());
    }

    PrologInteger decodeInteger(int base, CharSequence chars) {
        return new PrologInteger(new BigInteger(chars.toString(), base));
    }

    PrologInteger decodeInteger(int base, CharSequence chars, int offset) {
        return decodeInteger(base, chars.subSequence(offset, chars.length()));
    }
}
