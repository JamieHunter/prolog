// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.utility;

/**
 * This circular buffer has a number of interesting properties. It is generally used in a trailing buffer
 * scenario such as {@link prolog.io.InputBuffered}. If mark is not set, it typically means 'front of buffer'.
 * If mark is set, it identifies a read point into buffer. Content may exist before mark and after mark.
 */
public class CircularBuffer {
    private final char[] buffer;
    private int markPos; // location of buffered read, or -1 if not set / end of buffer
    private int endPos; // location of next write
    private int valid; // length of valid content

    /**
     * Create circular buffer of given size
     *
     * @param size Size of buffer
     */
    public CircularBuffer(int size) {
        this.buffer = new char[size];
        reset();
    }

    /**
     * @return number of characters in buffer that is considered valid
     */
    public int validLength() {
        return valid;
    }

    /**
     * @return Length of content from mark to end, or 0 if no mark
     */
    public int maxAdvance() {
        if (markPos < 0) {
            return 0;
        }
        int delta = endPos - markPos;
        if (delta <= 0) {
            delta += buffer.length;
        }
        return delta;
    }

    /**
     * @return Length of content from start to mark, or valid length if no mark
     */
    public int maxRewind() {
        return valid - maxAdvance();
    }

    /**
     * @return max readable chunk from mark to valid / end of buffer
     */
    public int chunkReadLength() {
        if (markPos < 0) {
            return 0;
        } else if (markPos < endPos) {
            return endPos - markPos;
        } else {
            return buffer.length - markPos;
        }
    }

    /**
     * @return max write chunk size that does not overwrite
     */
    public int chunkWriteLength() {
        if (markPos < 0 || markPos < endPos) {
            return buffer.length - endPos;
        } else {
            return markPos - endPos;
        }
    }

    /**
     * @return max writable length without overwriting
     */
    public int maxWriteLength() {
        if (markPos < 0) {
            return buffer.length;
        }
        return buffer.length - maxAdvance();
    }

    /**
     * @return array to use for read/write
     */
    public char[] array() {
        return buffer;
    }

    /**
     * @return position to use for read from mark
     */
    public int readOffset() {
        return markPos;
    }

    /**
     * @return position to use for write to advance end
     */
    public int writeOffset() {
        return endPos;
    }

    /**
     * Advance mark forward. Mark is allowed to wrap.
     *
     * @param delta adjustment of mark
     */
    public void advance(int delta) {
        if (delta > 0) {
            int max = maxAdvance();
            if (delta == max) {
                // considered to be at end
                setAtEnd();
            } else if (delta > max) {
                throw new UnsupportedOperationException("Cannot advance mark past end");
            } else {
                markPos += delta;
                if (markPos >= buffer.length) {
                    markPos -= buffer.length;
                }
            }
        } else if (delta < 0) {
            int max = maxRewind();
            if (-delta > max) {
                throw new UnsupportedOperationException("Cannot advance mark before start");
            }
            if (markPos < 0) {
                markPos = endPos + delta;
            } else {
                markPos += delta;
            }
            if (markPos < 0) {
                markPos += buffer.length;
            }
        }
    }

    /**
     * Consider mark to be at end of buffer
     */
    public void setAtEnd() {
        markPos = -1;
    }

    /**
     * Adjust number of characters effectively written, assumes count is valid,
     * overwriting is permitted. Caller is responsible for ensuring fill does not pass mark.
     */
    public void written(int count) {
        if (markPos < 0 && count > 0) {
            markPos = endPos;
        }
        endPos += count;
        valid += count;
        if (endPos >= buffer.length) {
            endPos -= buffer.length;
        }
        if (valid > buffer.length) {
            // overwriting occurred
            valid = buffer.length;
        }
    }

    /**
     * @return True if mark set
     */
    public boolean hasMark() {
        return markPos >= 0;
    }

    /**
     * Reset buffer to empty.
     */
    public void reset() {
        endPos = valid = 0;
        markPos = -1;
    }

    /**
     * Put a single character into buffer
     *
     * @param c Character to put
     */
    public void put(char c) {
        buffer[endPos] = c;
        written(1);
    }

    /**
     * Get a single character from buffer, mark position
     *
     * @return character, or -1 if at end
     */
    public int get() {
        if (markPos < 0) {
            return -1;
        }
        int c = buffer[markPos++];
        if (markPos == buffer.length) {
            markPos = 0;
        }
        if (markPos == endPos) {
            markPos = -1;
        }
        return c;
    }
}
