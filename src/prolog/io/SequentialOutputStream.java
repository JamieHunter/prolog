// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * PrologOutputStream that adapts a Java Output stream.
 */
public class SequentialOutputStream implements PrologOutputStream {

    private final OutputStream stream;
    private long pos = 0;

    public SequentialOutputStream(OutputStream stream) {
        this.stream = stream;
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
    public synchronized void write(int symbol) throws IOException {
        stream.write(symbol);
        pos++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(byte[] buffer, int off, int len) throws IOException {
        stream.write(buffer, off, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        stream.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        stream.close();
    }
}
