// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Optional;

/**
 * PrologInputStream/PrologOutputStream that wraps a Java Random access file.
 */
public class RandomAccessStream implements PrologInputStream, PrologOutputStream {

    private final FileChannel channel;
    // TODO: Smarter buffer management, magic value
    private final ByteBuffer buffer = ByteBuffer.allocate(512);
    private State state = State.CLEAN;

    public RandomAccessStream(FileChannel channel) {
        this.channel = channel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int read() throws IOException {
        if (fillBuffer()) {
            return buffer.get();
        } else {
            return IoUtility.EOF; // assume EOF
        }
    }

    private boolean fillBuffer() throws IOException {
        if (state == State.DIRTY_OUTPUT) {
            flush(); // commit any written data
        }
        if (state == State.HAS_INPUT && buffer.hasRemaining()) {
            return true;
        }
        buffer.clear();
        channel.read(buffer);
        buffer.flip();
        state = State.HAS_INPUT;
        return buffer.hasRemaining();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void write(int symbol) throws IOException {
        if (state == State.HAS_INPUT) {
            flush(); // discard any read data
        }
        buffer.put((byte) symbol);
        state = State.DIRTY_OUTPUT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void flush() throws IOException {
        switch (state) {
            case HAS_INPUT:
                // discard buffered data - fix position
                channel.position(channel.position() - buffer.remaining());
                buffer.clear();
                break;

            case DIRTY_OUTPUT:
                buffer.flip();
                channel.write(buffer);
                buffer.clear();
                break;
        }
        state = State.CLEAN;
        channel.force(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int available() throws IOException {
        long diff = channel.size() - logicalPosition();
        if (diff > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else if (diff < 0) {
            return 0;
        } else {
            return (int) diff;
        }
    }

    private long logicalPosition() throws IOException {
        switch (state) {
            case HAS_INPUT:
                return channel.position() - buffer.remaining();
            case DIRTY_OUTPUT:
                return channel.position() + buffer.remaining();
            default:
                return channel.position();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getPosition(Position position) throws IOException {
        position.setBytePos(logicalPosition());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean restorePosition(Position position) throws IOException {
        Optional<Long> pos = position.getBytePos();
        if (pos.isPresent()) {
            flush();
            channel.position(pos.get());
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() throws IOException {
        if (state != State.CLOSED) {
            flush();
            channel.close();
            state = State.CLOSED;
        }
    }

    private enum State {
        CLEAN,
        HAS_INPUT,
        DIRTY_OUTPUT,
        CLOSED
    }
}
