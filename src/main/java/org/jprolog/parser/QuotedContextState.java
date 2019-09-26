// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.parser;

import org.jprolog.constants.PrologAtom;
import org.jprolog.constants.PrologChars;
import org.jprolog.constants.PrologCodePoints;
import org.jprolog.constants.PrologInteger;
import org.jprolog.constants.PrologQuotedAtom;
import org.jprolog.constants.PrologString;
import org.jprolog.exceptions.PrologSyntaxError;
import org.jprolog.expressions.Term;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Class to handle a quoted atom/string/etc.
 */
/*package*/
class QuotedContextState extends ActiveParsingState {
    private final StringBuilder builder = new StringBuilder();
    private final String quote;
    private final boolean once;
    private final boolean escapes = tokenizer.options().characterEscapes;
    private LineMatcher lineMatcher;

    /**
     * Determine regex to use based on string quote.
     *
     * @param quote Opening quote
     * @return Pattern to use
     */
    private static Pattern selectString(String quote) {
        switch (quote.charAt(0)) {
            case '\'':
                return Tokenizer.SQ_STRING;
            case '\"':
                return Tokenizer.DQ_STRING;
            case '`':
                return Tokenizer.BQ_STRING;
            default:
                throw new InternalError("Unhandled string type");
        }
    }

    /**
     * Create a quoted context state to parse a single character code.
     *
     * @param tokenizer Owning tokenizer
     * @return Next state
     */
    static QuotedContextState newCharCode(Tokenizer tokenizer, LineMatcher top) {
        return new QuotedContextState(tokenizer, top, "'", Tokenizer.CHAR_PATTERN, true);
    }

    /**
     * Create a quoted context to parse quoted text (token, string, etc).
     *
     * @param tokenizer Owning tokenizer
     * @param quote     Opening quote character
     * @return Next state
     */
    static QuotedContextState newQuotedText(Tokenizer tokenizer, LineMatcher top, String quote) {
        return new QuotedContextState(tokenizer, top, quote, selectString(quote), false);
    }

    /**
     * Construct a string parser.
     *
     * @param tokenizer Owning tokenizer
     * @param quote     Opening quote character
     * @param pattern   Pattern to use
     * @param once      true if single character
     */
    private QuotedContextState(Tokenizer tokenizer, LineMatcher top, String quote, Pattern pattern, boolean once) {
        super(tokenizer);
        this.lineMatcher = top.split(pattern);
        this.quote = quote;
        this.once = once;
    }

    /**
     * Append text to buffer, and continue.
     *
     * @param text Text to add
     * @return self
     * @throws IOException on IO error
     */
    private ParseState proceed(String text) throws IOException {
        builder.append(text);
        return proceed();
    }

    /**
     * Proceed by progressing forward the match area past what was just matched.
     *
     * @return self
     * @throws IOException on IO error
     */
    private ParseState proceed() throws IOException {
        if (once && builder.length() > 0) {
            return end();
        } else {
            lineMatcher.next();
            return this;
        }
    }

    /**
     * End of quoted text. Turn into token
     *
     * @return {@link TokenFinishedState}
     * @throws IOException on IO error
     */
    private ParseState end() throws IOException {
        lineMatcher.end();
        if (once) {
            if (builder.length() > 0) {
                return ParseState.finish(new PrologInteger(builder.substring(0, 1).charAt(0)));
            } else {
                return tokenizer.parseReachedEOF();
            }
        } else {
            Term str;
            switch (quote) {
                case "`":
                    if (tokenizer.options().backquotedString) {
                        str = new PrologString(builder.toString());
                    } else {
                        str = new PrologChars(builder.toString());
                    }
                    break;
                case "'":
                    str = new PrologQuotedAtom(builder.toString());
                    break;
                default:
                    switch (tokenizer.options().doubleQuotes) {
                        case ATOM_symbol_char:
                        case ATOM_chars:
                            str = new PrologChars(builder.toString());
                            break;
                        case ATOM_codes:
                            str = new PrologCodePoints(builder.toString());
                            break;
                        case ATOM_atom:
                            str = new PrologAtom(builder.toString());
                            break;
                        default:
                            str = new PrologString(builder.toString());
                            break;
                    }
            }
            return ParseState.finish(str);
        }
    }

    /**
     * Quoted text spans lines, proceed to next line.
     *
     * @param text Either EOLN or empty.
     * @return self
     * @throws IOException on IO error
     */
    private ParseState nextLine(String text) throws IOException {
        builder.append(text);
        if (!tokenizer.newLine(lineMatcher)) {
            throw PrologSyntaxError.stringError(tokenizer.environment(), "EOF reached before terminating string: " + builder.toString());
        }
        return this;
    }

    /**
     * Iterate through string.
     *
     * @return Self or new state
     * @throws IOException on IO error
     */
    public ParseState next() throws IOException {
        if (!lineMatcher.scanNext()) {
            throw PrologSyntaxError.stringError(tokenizer.environment(), "Failed to parse: " + tokenizer.errorLine());
        }
        String match;
        match = lineMatcher.group(Tokenizer.STRING_CHAR_TAG);
        if (match != null) {
            return proceed(match);
        }
        if (escapes) {
            match = lineMatcher.group(Tokenizer.META_ESCAPE_TAG);
            if (match != null) {
                return proceed(match.substring(1));
            }
            match = lineMatcher.group(Tokenizer.CONTROL_ESCAPE_TAG);
            if (match != null) {
                char c;
                switch (match.substring(1)) {
                    //abrftnv
                    case "a":
                        c = '\007';
                        break;
                    case "b":
                        c = '\b';
                        break;
                    case "r":
                        c = '\r';
                        break;
                    case "f":
                        c = '\f';
                        break;
                    case "t":
                        c = '\t';
                        break;
                    case "n":
                        c = '\n';
                        break;
                    case "v":
                        c = '\013';
                        break;
                    default:
                        return proceed();
                }
                builder.append(c);
                return proceed();
            }
            match = lineMatcher.group(Tokenizer.CODE_ESCAPE_TAG);
            if (match != null) {
                String code = match.substring(1, match.length() - 1); // strip \...\
                if (code.startsWith("x")) {
                    int hex = Integer.parseInt(code.substring(1), 16);
                    builder.append((char) hex);
                } else {
                    int oct = Integer.parseInt(code, 8);
                    builder.append((char) oct);
                }
                return proceed();
            }
            match = lineMatcher.group(Tokenizer.BAD_BACKSLASH_TAG);
            if (match != null) {
                throw PrologSyntaxError.stringError(tokenizer.environment(), "Error parsing string at '" + match + "'");
            }
        }
        match = lineMatcher.group(Tokenizer.QUOTE_TAG);
        if (match != null) {
            return proceed(quote);
        }
        match = lineMatcher.group(Tokenizer.CATCH_ALL_TAG);
        if (match != null) {
            if (match.length() == 0) {
                // assume finish of line
                return nextLine("\n");
            }
            if (match.equals("\\") && escapes) {
                // assume \ before finish of line
                return nextLine("");
            }
            if (match.equals(quote) && !once) {
                // assume done
                return end();
            }
        }
        // anything else is accepted
        return proceed(lineMatcher.group());
    }
}
