// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.parser;

import prolog.constants.PrologEOF;
import prolog.constants.PrologFloat;
import prolog.constants.PrologInteger;
import prolog.exceptions.PrologError;
import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.flags.ReadOptions;
import prolog.io.PrologReadStream;
import prolog.variables.UnboundVariable;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Follow Prolog token syntax, producing token terms. An original version used ANTLR, however there is enough nuances
 * that ANTLR wasn't quite sufficient (such as ability to switch between syntactical parsing and consuming
 * individual characters). Conversely the Prolog grammer and features makes this a very practical
 * approach. A state machine is used for tokenization split into 4 key states - core parsing, string parsing,
 * line comment parsing and block comment parsing.
 */
public final class Tokenizer extends TokenRegex {

    private static final int MAX_LINE_SIZE = 1024; // Arbitrary.

    private final Environment environment;
    private final ReadOptions options;
    private final PrologReadStream prologStream; // For better error messages
    private final BufferedReader javaStream; // Java reader
    private final Map<String, UnboundVariable> variableMap = new HashMap<>();

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
     * @param environment  Execution environment.
     * @param options      Options to control tokenization and general parsing.
     * @param prologStream Prolog stream
     */
    public Tokenizer(Environment environment, ReadOptions options, PrologReadStream prologStream) {
        this.environment = environment;
        this.options = options;
        this.prologStream = prologStream;
        this.javaStream = prologStream.javaReader();
    }

    /**
     * Build tokenizer for environment reading from a prolog stream, default options
     *
     * @param environment  Execution environment.
     * @param prologStream Prolog stream
     */
    public Tokenizer(Environment environment, PrologReadStream prologStream) {
        this(environment, new ReadOptions(environment, null), prologStream);
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
            mark(); // note start to allow rewind
            ParseState state = readLineAndParseToken(); // initial state

            //
            // Run state-machine to parse a token
            while (!state.done()) {
                state = state.next();
            }
            return state.term();
        } catch (IOException ioe) {
            throw PrologError.systemError(environment, ioe);
        }
    }

    /**
     * State machine - normal parsing, accept any token.
     *
     * @return next state
     * @throws IOException on IO Error
     */
    ParseState readLineAndParseToken() throws IOException {
        String line = readLine();
        if (line == null) {
            return parseReachedEOF();
        } else {
            return parseAnyToken(line);
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
     * @param line Line being parsed
     * @return First state
     */
    ParseState parseAnyToken(String line) {
        return new TokenParseCoreState(this, line);
    }

    /**
     * Sets mark at current cursor position (i.e. character next to be read).
     *
     * @throws IOException IO Error
     */
    void mark() throws IOException {
        javaStream.mark(MAX_LINE_SIZE + 16);
    }

    /**
     * Consider text between mark and finish of match as having been used.
     * Mark/cursor is moved to after the match. It is important that
     * the matcher is for the text following mark.
     *
     * @param matcher Matcher successful match
     * @throws IOException IO Exception
     */
    void consume(Matcher matcher) throws IOException {
        consume(matcher.end());
    }

    /**
     * Consider text after mark as having been used.
     * Mark/cursor is moved to after the match.
     *
     * @param chars Number of characters after match to consume
     * @throws IOException IO Exception
     */
    void consume(int chars) throws IOException {
        javaStream.reset();
        javaStream.skip(chars);
        mark();
    }

    /**
     * Read text from mark to finish of line. Mark is not moved. This is in preparation for the next match attempt.
     *
     * @return Text to finish of line, or null if finish of file
     * @throws IOException IO Exception
     */
    String readLine() throws IOException {
        return javaStream.readLine();
    }

    /**
     * Test to see if next character matches the one given
     *
     * @param test Character to test
     * @return true if bracket
     */
    public boolean isNext(char test) {
        try {
            mark();
            int c = javaStream.read();
            javaStream.reset();
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
        try {
            for (; ; ) {
                mark();
                int c = javaStream.read();
                if (c < 0 || c == '\n') {
                    return;
                }
                if (c != ' ' && c != '\r' && c != '\t') {
                    javaStream.reset();
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

    Term parseVariable(String text) {
        return variableMap.computeIfAbsent(text,
                n -> new UnboundVariable(n, environment.nextVariableId()));
    }

    Term parseAnonymousVariable(String text) {
        if (text.equals("_")) {
            return new UnboundVariable("_", environment.nextVariableId());
        } else {
            throw new UnsupportedOperationException("NYI");
        }
    }

    PrologInteger decodeInteger(int base, CharSequence chars) {
        return new PrologInteger(new BigInteger(chars.toString(), base));
    }

    PrologInteger decodeInteger(int base, CharSequence chars, int offset) {
        return decodeInteger(base, chars.subSequence(offset, chars.length()));
    }
}
