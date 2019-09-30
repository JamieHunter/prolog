// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.io;

import java.io.IOException;

public final class IoUtility {

    public static final int EOF = -1;

    private IoUtility() {
        // do not construct
    }

    /**
     * Call reader to fill as many entries in buffer as possible (Symbols).
     *
     * @param reader Reader function that returns -1 for EOF, else a symbol.
     * @param buffer Buffer to be filled with symbols
     * @param off    Offset into buffer to start
     * @param len    Number of symbols to read (max)
     * @return Number of symbols read, or -1 if EOF
     * @throws IOException on IO Error
     */
    /*package*/
    static int slowRead(IoRead reader, char[] buffer, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            int c = reader.read();
            if (c < 0) {
                return i == 0 ? c : i;
            }
            buffer[off++] = (char) c;
        }
        return len;
    }

    /**
     * Call reader to fill as many entries in buffer as possible when stream is assumed to be a byte stream.
     *
     * @param reader Reader function that returns -1 for EOF, else a byte.
     * @param buffer Buffer to be filled with bytes
     * @param off    Offset into buffer to start
     * @param len    Number of bytes to read (max)
     * @return Number of bytes read, or -1 if EOF
     * @throws IOException on IO Error
     */
    /*package*/
    static int slowRead(IoRead reader, byte[] buffer, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            int c = reader.read();
            if (c < 0) {
                return i == 0 ? c : i;
            }
            buffer[off++] = (byte) c;
        }
        return len;
    }

    /**
     * Call writer to write everything from buffer (symbols).
     *
     * @param writer Writer function to write symbol
     * @param buffer Source of symbols
     * @param off    Offset into buffer to start
     * @param len    Number of symbols to write
     * @throws IOException on IO Error
     */
    /*package*/
    static void slowWrite(IoWrite writer, char[] buffer, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            int c = buffer[off++] & Integer.MAX_VALUE;
            writer.write(c);
        }
    }

    /**
     * Call writer to write everything from buffer (bytes).
     *
     * @param writer Writer function to write byte
     * @param buffer Source of bytes
     * @param off    Offset into buffer to start
     * @param len    Number of bytes to write
     * @throws IOException on IO Error
     */
    /*package*/
    static void slowWrite(IoWrite writer, byte[] buffer, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            int c = buffer[off++];
            writer.write(c);
        }
    }

    /**
     * Utility to call read method until end of line reached
     *
     * @param reader reader function
     * @return Read string, or null if EOF reached before any text read
     * @throws IOException io error
     */
    public static String readLine(IoRead reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (; ; ) {
            int c = reader.read();
            if (c == '\n') {
                return builder.toString();
            }
            if (c == EOF) {
                return builder.length() == 0 ? null : builder.toString();
            }
            builder.append((char) c);
        }
    }

}
