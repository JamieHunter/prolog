// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.io;

import org.jprolog.flags.CloseOptions;

import java.io.IOException;

/**
 * Base class for output stream filters. As implemented, it is pass-through.
 */
public class FilteredOutputStream implements PrologOutputStream {

    private final PrologOutputStream stream;

    public FilteredOutputStream(PrologOutputStream stream) {
        this.stream = stream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getPosition(Position position) throws IOException {
        stream.getPosition(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean seekPosition(Position position) throws IOException {
        return stream.seekPosition(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKnownPosition(Position position) {
        stream.setKnownPosition(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(int symbol) throws IOException {
        stream.write(symbol);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(char[] buffer, int off, int len) throws IOException {
        stream.write(buffer, off, len);
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
    public void close(CloseOptions options) throws IOException {
        stream.close(options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void seekEndOfStream() throws IOException {
        stream.seekEndOfStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean approveClose(CloseOptions options) {
        return stream.approveClose(options);
    }
}
