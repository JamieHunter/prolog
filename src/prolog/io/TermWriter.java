// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import prolog.expressions.Term;
import prolog.parser.TokenRegex;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Format a term (base class) to {@link WriteContext}.
 * @param <T> Kind of Term written by this Writer.
 */
public abstract class TermWriter<T extends Term> extends TokenRegex {
    protected final WriteContext context;
    protected final T term;
    protected final Writer writer;

    /**
     * Create writer utility.
     * @param context Write context
     * @param term Term being written
     */
    protected TermWriter(WriteContext context, T term) {
        this.context = context;
        this.term = term;
        this.writer = context.writer();
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
    public abstract void write() throws IOException;

    /**
     * Format a string correctly quoted
     * @param quote Style of quotes to apply
     * @param text text to convert
     * @throws IOException IO Error
     */
    public void writeQuoted(char quote, String text) throws IOException {
        context.beginQuoted();
        writer.write(quote);
        Matcher m = STRING_SPLITTER.matcher(text);
        for(;m.lookingAt();m.region(m.end(), m.regionEnd())) {
            String x = m.group(PRINTABLE_TAG);
            if (x != null) {
                writer.write(x);
                continue;
            }
            x = m.group(QUOTE_TAG);
            if (x != null) {
                if (x.charAt(0) == quote) {
                    writer.write(x + x);
                } else {
                    writer.write(x);
                }
                continue;
            }
            x = m.group(BACKSLASH_TAG);
            if (x != null) {
                writer.write("\\\\");
            }
            x = m.group(ALL_TAG);
            if (x != null) {
                switch(x.charAt(0)) {
                    case '\002':
                        writer.write("\\a");
                        break;
                    case '\b':
                        writer.write("\\b");
                        break;
                    case '\n':
                        writer.write("\\n");
                        break;
                    case '\f':
                        writer.write("\\f");
                        break;
                    case '\r':
                        writer.write("\\r");
                        break;
                    case '\t':
                        writer.write("\\t");
                        break;
                    case '\013':
                        writer.write("\\v");
                        break;
                    default:
                        if (x.charAt(0) < 0x20) {
                            writer.write(String.format("\\%03o\\", (int)x.charAt(0)));
                        } else {
                            writer.write(String.format("\\x%x\\", (int)x.charAt(0)));
                        }
                        break;
                }
            }
        }
        writer.write(quote);
    }
}
