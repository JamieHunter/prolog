// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.io;

import java.io.IOException;

/**
 * Track line/column position
 */
public class InputPositionTracker extends FilteredInputStream {
    private final PositionTracker tracker;

    /**
     * Create new filter
     *
     * @param stream Stream being wrapped
     */
    public InputPositionTracker(PrologInputStream stream, PositionTracker tracker) {
        super(stream);
        this.tracker = tracker;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {
        int c = super.read();
        tracker.visit(c);
        return c;
    }

    @Override
    public int read(char[] b, int off, int len) throws IOException {
        int actLen = super.read(b, off, len);
        tracker.visit(b, off, actLen);
        return actLen;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int actLen = super.read(b, off, len);
        tracker.visit(b, off, actLen);
        return actLen;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getPosition(Position position) throws IOException {
        super.getPosition(position);
        tracker.getPosition(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean seekPosition(Position position) throws IOException {
        if (super.seekPosition(position)) {
            setKnownPosition(position);
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKnownPosition(Position position) {
        super.setKnownPosition(position);
        tracker.setKnownPosition(position);
    }
}
