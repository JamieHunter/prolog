package org.jprolog.test.internal;

import org.jprolog.flags.CloseOptions;
import org.jprolog.io.PrologOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Behaves as a stream, but writes to a buffered string only writing to output if an error is reported.
 */
public class OutputMonitor implements PrologOutputStream {
    private final StringBuilder accumulator = new StringBuilder();
    private final ByteArrayOutputStream lineBuilder = new ByteArrayOutputStream();
    private final Pattern grep;
    private int count = 0;
    private boolean silent = false;

    public OutputMonitor(String grep) {
        if (grep.startsWith("-")) {
            silent = true;
            grep = grep.substring(1);
        }
        this.grep = Pattern.compile(grep);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(int symbol) throws IOException {
        switch (symbol) {
            case '\r':
                break;
            case '\n':
                endOfLine();
                break;
            default:
                lineBuilder.write(symbol);
                break;

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close(CloseOptions options) throws IOException {
        endOfLine();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void seekEndOfStream() throws IOException {

    }

    /**
     * Process end of line.
     */
    public void endOfLine() {
        flushText();
        if (count > 0) {
            System.out.println();
        }
        accumulator.append('\n');
    }

    /**
     * Flush text from line buffer, grepping in the process
     */
    public void flushText() {
        if (lineBuilder.size() == 0) {
            return;
        }
        byte[] line = lineBuilder.toByteArray();
        lineBuilder.reset();
        String text = new String(line, StandardCharsets.UTF_8);
        if (grep.matcher(text).find(0)) {
            count++;
            if (count == 1 && !silent) {
                System.out.print(accumulator.toString());
            }
        }
        if (count > 0 && !silent) {
            System.out.print(text);
        }
        accumulator.append(text);
    }

    /**
     * Get number of lines matching grep pattern
     * @return Count
     */
    public int getCount() {
        flushText();
        return count;
    }

    @Override
    public String toString() {
        return accumulator.toString();
    }

    public String flushString() {
        flushText();
        return accumulator.toString();
    }
}
