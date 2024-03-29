// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.io;

import org.jprolog.flags.CloseOptions;

import java.io.IOException;
import java.io.InputStream;

/**
 * PrologInputStream that adapts a Java Input stream.
 */
public class SequentialInputStream implements PrologInputStream {

    private final InputStream stream;
    private long pos = 0;
    private boolean closed = false;

    public SequentialInputStream(InputStream stream) {
        this.stream = stream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int read() throws IOException {
        int c = stream.read();
        if (c < 0) {
            return IoUtility.EOF;
        } else {
            pos++; // position after read
            return c;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] buffer, int off, int len) throws IOException {
        return stream.read(buffer, off, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int available() throws IOException {
        return stream.available();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void getPosition(Position position) {
        position.setBytePos(pos); // lowest level only
        position.setCharPos(pos); // caller will overwrite
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close(CloseOptions options) throws IOException {
        if (!closed) {
            stream.close();
            closed = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void seekEndOfStream() throws IOException {
        throw new IOException("Cannot seek");
    }
}
