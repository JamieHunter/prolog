// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface for Prolog output streams. It is similar to Java's output stream, however there are enough differences that
 * it is better to create interface than subclass {@link OutputStream}. Note that output stream operates in terms
 * of symbols not bytes. A specific implementation may operate in terms of bytes or characters.
 */
public interface PrologOutputStream extends PrologStream, IoWrite {

    /**
     * Write text to stream
     *
     * @param text Text to write
     * @throws IOException on IO Error
     */
    default void write(String text) throws IOException {
        char[] array = text.toCharArray();
        write(array, 0, array.length);
    }

    /**
     * Write an array of symbols. Symbols are bytes or characters.
     *
     * @param buffer Array of symbols
     * @param off    Offset into array
     * @param len    Number of symbols to write
     * @throws IOException on IO Error
     */
    default void write(char[] buffer, int off, int len) throws IOException {
        IoUtility.slowWrite(this, buffer, off, len);
    }

    /**
     * Write an array of bytes.
     *
     * @param buffer Array of bytes
     * @param off    Offset into array
     * @param len    Number of bytes to write
     * @throws IOException on IO Error
     */
    default void write(byte[] buffer, int off, int len) throws IOException {
        IoUtility.slowWrite(this, buffer, off, len);
    }

    /**
     * Flush output
     *
     * @throws IOException on IO Error
     */
    default void flush() throws IOException {
    }
}
