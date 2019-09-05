// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import prolog.flags.CloseOptions;
import prolog.utility.CircularBuffer;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter to handle buffered rewindable input. Logic uses a trailing buffer approach. Buffer is rewound by
 * restoring a saved position.
 */
public class InputBuffered extends FilteredInputStream {
    private final CircularBuffer buf;
    private long charPos = 0; // effective character position
    private final static int DEFAULT_BUFFER = 8192;
    private final static int MIN_BUFFER = 2;

    public InputBuffered(PrologInputStream stream, int bufferSize) {
        super(stream);
        if (bufferSize < MIN_BUFFER) {
            bufferSize = DEFAULT_BUFFER;
        }
        buf = new CircularBuffer(bufferSize);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int advance(int len) throws IOException {
        int counted = 0;
        while (len > 0) {
            if (buf.hasMark()) {
                // consume from buffer first
                int available = buf.maxAdvance();
                if (available >= len) {
                    // entirely from buffer
                    buf.advance(len);
                    charPos += len;
                    return counted + len;
                } else {
                    // empty buffer
                    charPos += available;
                    counted += available;
                    len -= available;
                    buf.setAtEnd();
                }
            }
            int readMoreLen = readMore(len);
            if (readMoreLen < 0) {
                return IoUtility.EOF;
            }
        }
        return counted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int read() throws IOException {
        if (!buf.hasMark()) {
            // read one character into buffer
            int i = super.read();
            if (i < 0) {
                return i;
            }
            buf.put((char) i);
        }
        charPos++;
        return buf.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(char[] b, int off, int len) throws IOException {
        if (!buf.hasMark()) {
            // at end, need to write more data into buffer (mark is set)
            int freshLen = readMore(len);
            if (freshLen < 0) {
                return IoUtility.EOF;
            }
        }
        // consume from buffer, at least one character
        int copyLen = Math.min(len, buf.chunkReadLength());
        System.arraycopy(buf.array(), buf.readOffset(), b, off, copyLen);
        buf.advance(copyLen);
        charPos += copyLen;
        return copyLen;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int available() throws IOException {
        long available = (long) super.available() + (long) buf.maxAdvance();
        if (available > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return (int) available;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close(CloseOptions options) throws IOException {
        super.close(options);
        buf.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getPosition(Position position) throws IOException {
        position.setCharPos(charPos);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean seekPosition(Position position) throws IOException {
        // Note, this does not call parent. Byte position meaningless with this filter
        Optional<Long> setPos = position.getCharPos();
        if (!setPos.isPresent()) {
            return false; // cannot restore
        }
        long targetPos = setPos.get();
        if (targetPos >= charPos) {
            if (targetPos - charPos > Integer.MAX_VALUE) {
                // TODO: exception or false?
                throw new IndexOutOfBoundsException("Seek position error");
            }
            // handle through advance
            advance((int) (targetPos - charPos));
        } else {
            long delta = charPos - targetPos;
            if (delta > buf.maxRewind()) {
                // TODO: exception or false?
                throw new IndexOutOfBoundsException("Seek position error");
            }
            buf.advance(-(int) delta);
        }
        charPos = targetPos;
        return true;
    }

    /**
     * Attempts to read more into the buffer. Typically called only when buffer is empty
     *
     * @param len Number of characters to read
     * @return number of characters read
     */
    private int readMore(int len) throws IOException {
        len = Math.min(len, buf.chunkWriteLength());
        int count = super.read(buf.array(), buf.writeOffset(), len);
        if (count < 0) {
            count = buf.maxAdvance();
            return count == 0 ? IoUtility.EOF : count;
        }
        buf.written(count);
        return buf.maxAdvance();
    }
}
