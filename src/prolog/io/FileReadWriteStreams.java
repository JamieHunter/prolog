// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Optional;

/**
 * PrologInputStream/PrologOutputStream that wraps a Java file channel. This exposes two Prolog streams,
 * one for writing, one for reading, both accessing the same mutual file
 */
public class FileReadWriteStreams implements PrologInputStream, PrologOutputStream {

    private final FileChannel channel;
    // TODO: Smarter buffer management, magic value
    private final ByteBuffer buffer = ByteBuffer.allocate(512);
    private State state = State.CLEAN;

    public FileReadWriteStreams(FileChannel channel) {
        this.channel = channel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int read() throws IOException {
        ensureReading();
        if (!readSome()) {
            return IoUtility.EOF; // assume EOF
        }
        return buffer.get() & 0xff;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] bbuf, int off, int len) throws IOException {
        ensureReading();
        if (!readSome()) {
            return IoUtility.EOF; // assume EOF
        }
        if (len <= 0) {
            return len;
        }
        int resLen = Math.min(len, buffer.remaining());
        buffer.get(bbuf, off, resLen);
        return resLen;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void write(int symbol) throws IOException {
        ensureWriting();
        conditionalCommitWrite();
        buffer.put((byte) symbol);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void write(byte[] bbuf, int off, int len) throws IOException {
        ensureWriting();
        while(len > 0) {
            conditionalCommitWrite();
            int chunkLen = Math.min(len, buffer.remaining());
            buffer.put(bbuf, off, chunkLen);
            off += chunkLen;
            len -= chunkLen;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void flush() throws IOException {
        switch (state) {
            case HAS_INPUT:
                discardRead();
                break;

            case DIRTY_OUTPUT:
                commitWrite();
                break;
        }
        buffer.clear();
        state = State.CLEAN;
        channel.force(true);
    }

    private void ensureReading() throws IOException {
        switch (state) {
            case HAS_INPUT:
                return;
            case CLEAN:
                state = State.HAS_INPUT;
                buffer.flip();
                break;
            case DIRTY_OUTPUT:
                commitWrite();
                state = State.HAS_INPUT;
                break;
            case CLOSED:
                throw new IOException("Channel is closed");
            default:
                throw new IOException("Invalid state");
        }
    }

    private void ensureWriting() throws IOException {
        switch (state) {
            case DIRTY_OUTPUT:
                return;
            case CLEAN:
                state = State.DIRTY_OUTPUT;
                break;
            case HAS_INPUT:
                discardRead();
                state = State.DIRTY_OUTPUT;
                break;
            case CLOSED:
                throw new IOException("Channel is closed");
            default:
                throw new IOException("Invalid state");
        }
    }

    private void discardRead() throws IOException {
        assert state == State.HAS_INPUT;
        channel.position(channel.position() - buffer.remaining());
        buffer.clear();
        state = State.CLEAN;
    }

    /**
     * Write content of buffer to file
     * @throws IOException If IO error occurs
     */
    private void commitWrite() throws IOException {
        assert state == State.DIRTY_OUTPUT;
        if (buffer.position() > 0) {
            buffer.flip();
            while(buffer.hasRemaining()) {
                channel.write(buffer);
            }
            buffer.clear();
        }
    }

    private void conditionalCommitWrite() throws IOException {
        assert state == State.DIRTY_OUTPUT;
        if (!buffer.hasRemaining()) {
            commitWrite();
        }
    }

    /**
     * Fill empty buffer providing something to read
     * @throws IOException If IO error occurs
     */
    private boolean readSome() throws IOException {
        assert state == State.HAS_INPUT;
        if (buffer.hasRemaining()) {
            return true;
        }
        buffer.clear();
        channel.read(buffer);
        buffer.flip();
        return buffer.hasRemaining();
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
                return channel.position() + buffer.position();
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
