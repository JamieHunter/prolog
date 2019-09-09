// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import prolog.flags.CloseOptions;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;

/**
 * Stream that writes to a string.
 */
public class StringOutputStream implements PrologOutputStream, Closeable {

    private final StringWriter writer = new StringWriter();
    private boolean closed = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(int symbol) throws IOException {
        writer.write(symbol);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(String text) throws IOException {
        writer.write(text);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(char[] buffer, int off, int len) throws IOException {
        writer.write(buffer, off, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close(CloseOptions options) throws IOException {
        close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            writer.close();
            closed = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return writer.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void seekEndOfStream() throws IOException {
        throw new IOException("Cannot seek");
    }
}
