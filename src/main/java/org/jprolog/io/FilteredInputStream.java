// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.io;

import org.jprolog.flags.CloseOptions;

import java.io.IOException;

/**
 * Base class for input stream filters. As implemented, it is pass-through.
 */
public class FilteredInputStream implements PrologInputStream {

    private final PrologInputStream stream;

    public FilteredInputStream(PrologInputStream stream) {
        this.stream = stream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPrompt(Prompt prompt) {
        stream.setPrompt(prompt);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showPrompt() throws IOException {
        stream.showPrompt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {
        return stream.read();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(char[] b, int off, int len) throws IOException {
        return stream.read(b, off, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return stream.read(b, off, len);
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
}
