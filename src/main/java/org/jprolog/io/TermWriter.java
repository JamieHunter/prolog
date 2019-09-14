// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.io;

import org.jprolog.expressions.Term;
import org.jprolog.parser.TokenRegex;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Format a term (base class) to {@link WriteContext}.
 * @param <T> Kind of Term written by this Writer.
 */
public abstract class TermWriter<T extends Term> extends TokenRegex {
    protected final WriteContext context;
    protected final PrologOutputStream output;

    /**
     * Create writer utility.
     * @param context Write context
     */
    protected TermWriter(WriteContext context) {
        this.context = context;
        this.output = context.output();
    }

    /**
     * General quoting rules.
     */
    private static final String BACKSLASH_TAG = "b";
    private static final String PRINTABLE_TAG = "p";
    private static final String QUOTE_TAG = "q";
    private static final String ALL_TAG = "a";

    // Regex looks for safe and unsafe characters in the string
    private static final Pattern STRING_SPLITTER = Pattern.compile(
            or(
                    group(QUOTE_TAG, "['`\"]"),
                    group(BACKSLASH_TAG, "\\\\"),
                    group(PRINTABLE_TAG, "[\\x20-\\x7E]++"),
                    group(ALL_TAG, ".")
            ), Pattern.DOTALL
    );

    /**
     * Override with actual write functionality.
     * @throws IOException Thrown if there is an IO error
     */
    public abstract void write(Term term) throws IOException;

    /**
     * Format a string correctly quoted
     * @param quote Style of quotes to apply
     * @param text text to convert
     * @throws IOException IO Error
     */
    public void writeQuoted(char quote, String text) throws IOException {
        context.beginQuoted();
        output.write(quote);
        Matcher m = STRING_SPLITTER.matcher(text);
        for(;m.lookingAt();m.region(m.end(), m.regionEnd())) {
            String x = m.group(PRINTABLE_TAG);
            if (x != null) {
                output.write(x);
                continue;
            }
            x = m.group(QUOTE_TAG);
            if (x != null) {
                if (x.charAt(0) == quote) {
                    output.write(x + x);
                } else {
                    output.write(x);
                }
                continue;
            }
            x = m.group(BACKSLASH_TAG);
            if (x != null) {
                output.write("\\\\");
            }
            x = m.group(ALL_TAG);
            if (x != null) {
                switch(x.charAt(0)) {
                    case '\002':
                        output.write("\\a");
                        break;
                    case '\b':
                        output.write("\\b");
                        break;
                    case '\n':
                        output.write("\\n");
                        break;
                    case '\f':
                        output.write("\\f");
                        break;
                    case '\r':
                        output.write("\\r");
                        break;
                    case '\t':
                        output.write("\\t");
                        break;
                    case '\013':
                        output.write("\\v");
                        break;
                    default:
                        if (x.charAt(0) < 0x20) {
                            output.write(String.format("\\%03o\\", (int)x.charAt(0)));
                        } else {
                            output.write(String.format("\\x%x\\", (int)x.charAt(0)));
                        }
                        break;
                }
            }
        }
        output.write(quote);
    }
}
