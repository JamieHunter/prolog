// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import java.io.IOException;

/**
 * Track line/column position
 */
public class OutputPositionTracker extends FilteredOutputStream {
    private final PositionTracker tracker;

    /**
     * Create new filter
     *
     * @param stream Stream being wrapped
     */
    public OutputPositionTracker(PrologOutputStream stream, PositionTracker tracker) {
        super(stream);
        this.tracker = tracker;
    }

    @Override
    public void write(int c) throws IOException {
        super.write(c);
        tracker.visit(c);
    }

    @Override
    public void write(char[] buffer, int off, int len) throws IOException {
        super.write(buffer, off, len);
        tracker.visit(buffer, off, len);
    }

    @Override
    public void write(byte[] buffer, int off, int len) throws IOException {
        super.write(buffer, off, len);
        tracker.visit(buffer, off, len);
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
