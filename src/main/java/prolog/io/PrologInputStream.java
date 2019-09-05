// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for Prolog input streams. There is a lot of overlap between Prolog Input Stream's and Java Input Streams
 * but enough differences that it did not make sense to inherit from Java's {@link InputStream}. Note this interface
 * operates in terms of symbols, which may be bytes or characters.
 */
public interface PrologInputStream extends PrologStream, IoRead {

    /**
     * For interactive streams, change the current prompt.
     *
     * @param prompt Prompt to show.
     */
    default void setPrompt(Prompt prompt) {
    }

    /**
     * For interactive streams, show the prompt
     * @throws IOException on IO Error
     */
    default void showPrompt() throws IOException {
    }

    /**
     * Advance input by length symbols.
     * @param len Length to advance
     * @return length advanced, or -1 if EOF reached before any advancing was possible
     * @throws IOException on IO Error
     */
    default int advance(int len) throws IOException {
        for(int i = 0; i < len; i++) {
            if (read() == IoUtility.EOF) {
                return i == 0 ? IoUtility.EOF : i;
            }
        }
        return len;
    }

    /**
     * Read a sequence of symbols into a buffer. Return -1 if EOF reached
     * before reading anything. Value > 0 if anything read. 0 may be returned
     * if len == 0. Symbols may be bytes or characters.
     *
     * @param buffer Buffer to read into
     * @param off    Offset into buffer
     * @param len    Length to read (buffer length must be at least off+len)
     * @return read length, or -1 if EOF, or len if len <= 0.
     * @throws IOException on IO Error
     */
    default int read(char[] buffer, int off, int len) throws IOException {
        return IoUtility.slowRead(this, buffer, off, len);
    }

    /**
     * Read a sequence of bytes into a buffer. Return -1 if EOF reached
     * before reading anything. Value > 0 if anything read. 0 may be returned
     * if len == 0. This can be called if (and only if) it is known that the
     * symbols are actually bytes.
     *
     * @param buffer Buffer to read into
     * @param off    Offset into buffer
     * @param len    Length to read (buffer length must be at least off+len)
     * @return read length, or -1 if EOF, or len if len <= 0.
     * @throws IOException on IO Error
     */
    default int read(byte[] buffer, int off, int len) throws IOException {
        return IoUtility.slowRead(this, buffer, off, len);
    }

    /**
     * Read a sequence of symbols into a buffer until end of line marker read.
     * Drop end of line marker. Return null if EOF.
     *
     * @return read text, or null if EOF reached before any text read.
     * @throws IOException on IO Error
     */
    default String readLine() throws IOException {
        return IoUtility.readLine(this);
    }

    /**
     * @return estimated number of symbols available, <= {@link Integer#MAX_VALUE}. Returns
     * {@link Integer#MAX_VALUE} if number of symbols available is indeterminate.
     * @throws IOException on IO Error
     */
    default int available() throws IOException {
        return Integer.MAX_VALUE;
    }

}
