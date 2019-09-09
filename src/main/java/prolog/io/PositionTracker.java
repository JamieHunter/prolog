// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import prolog.flags.CloseOptions;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Track line/column position
 */
public class PositionTracker implements PrologStream {
    private long linePos = 0;
    private long columnPos = 0;
    private boolean startOfLine = true;
    private static final Consumer<PositionTracker>[] handler = new Consumer[0x20];

    private static final Consumer<PositionTracker> noop = o->{};
    private static final Consumer<PositionTracker> nl = o->{
        o.startOfLine = true;
    };
    private static final Consumer<PositionTracker> tab = o->{
        o.columnPos += 8 - (o.columnPos & 7);
    };

    static {
        for(int i = 0; i < handler.length; i++) {
            handler[i] = noop;
        }
        handler['\t'] = tab;
        handler['\n'] = nl;
    }

    public PositionTracker() {
    }

    private void handleStartOfLine() {
        if (startOfLine) {
            linePos++;
            columnPos = 0;
            startOfLine = false;
        }
    }

    /**
     * Given a single character, increment columns and lines
     * @param c Character
     */
    public synchronized void visit(int c) {
        handleStartOfLine();
        if (c >= 0x20) {
            columnPos++;
        } else if (c >= 0) {
            handler[c].accept(this);
        }
    }

    /**
     * Given an array of characters, increment columns and lines
     * @param chars Array of characters
     * @param start starting index
     * @param len number of characters
     */
    public synchronized void visit(char [] chars, int start, int len) {
        if (len <= 0) {
            return;
        }
        handleStartOfLine();
        int end = start + len;
        for(int i = start; i < end; i++) {
            char c = chars[i];
            if (c >= 0x20) {
                columnPos++;
            } else {
                handler[c].accept(this);
            }
        }
    }

    /**
     * Given an array of bytes, increment columns and lines
     * @param bytes Array of characters
     * @param start starting index
     * @param len number of characters
     */
    public void visit(byte [] bytes, int start, int len) {
        if (len <= 0) {
            return;
        }
        handleStartOfLine();
        int end = start + len;
        for(int i = start; i < end; i++) {
            byte c = bytes[i];
            if (c >= 0x20) {
                columnPos++;
            } else {
                handler[c].accept(this);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getPosition(Position position) throws IOException {
        handleStartOfLine();
        position.setLinePos(linePos);
        position.setColumnPos(columnPos);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean seekPosition(Position position) throws IOException {
        setKnownPosition(position);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKnownPosition(Position position) {
        startOfLine = false;
        position.getLinePos().ifPresent(c -> linePos = c);
        position.getColumnPos().ifPresent(c -> columnPos = c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close(CloseOptions options) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void seekEndOfStream() throws IOException {
    }
}
