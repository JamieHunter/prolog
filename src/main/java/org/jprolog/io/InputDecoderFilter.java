// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * Decode multi-byte character sets (UTF etc). This is based on {@link sun.nio.cs.StreamDecoder}.
 */
public class InputDecoderFilter extends FilteredInputStream {
    private static final int NO_CHAR = -2;
    private static final int DEFAULT_BYTE_BUFFER_SIZE = 8192;
    private int nextChar = NO_CHAR;
    private final CharsetDecoder decoder;
    private final ByteBuffer bb;
    private long charPos = 0; // effective character position

    public InputDecoderFilter(PrologInputStream stream, Charset charset) {
        super(stream);
        decoder = charset.newDecoder().
                onMalformedInput(CodingErrorAction.REPLACE).
                onUnmappableCharacter(CodingErrorAction.REPLACE);
        bb = ByteBuffer.allocate(DEFAULT_BYTE_BUFFER_SIZE);
        bb.flip();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int read() throws IOException {
        switch (this.nextChar) {
            case NO_CHAR:
                // Read a pair of characters if possible
                char[] pair = new char[2];
                int count = this.readImpl(pair, 0, 2);
                switch (count) {
                    case 2:
                        // actually read two characters
                        // place one ready for next read()
                        this.nextChar = pair[1];
                        charPos++;
                        return pair[0];
                    case 1:
                        // single character read
                        charPos++;
                        return pair[0];
                    default:
                        // reached EOF
                        this.nextChar = IoUtility.EOF;
                        return IoUtility.EOF;
                }
            case IoUtility.EOF:
                // Already at EOF
                return IoUtility.EOF;
            default:
                // Use the buffered character
                return consume();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int read(char[] b, int off, int len) throws IOException {
        if (this.nextChar == IoUtility.EOF) {
            // Already at EOF
            return IoUtility.EOF;
        } else if (len <= 0) {
            return len;
        } else {
            // First consider the buffered character
            int counted = 0;
            if (this.nextChar >= 0) {
                b[off] = consume();
                ++off;
                --len;
                counted = 1;
                if (len == 0) {
                    return counted;
                }
            }

            if (len == 1) {
                // Handle case when only one (more) character is requested
                // This will recurse back into read to read 2 characters
                int single = this.read();
                if (single == -1) {
                    return counted == 0 ? -1 : counted;
                } else {
                    charPos++;
                    b[off] = (char) single;
                    return counted + 1;
                }
            } else {
                // fill buffer, multiple bytes
                int sublen = this.readImpl(b, off, len);
                if (sublen < 0) {
                    nextChar = IoUtility.EOF;
                    return counted == 0 ? -1 : counted;
                } else {
                    charPos += sublen;
                    return counted + sublen;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getPosition(Position position) {
        position.setCharPos(charPos);
    }

    /**
     * {@inheritDoc}
     */
    public boolean seekPosition(Position position) throws IOException {
        if (super.seekPosition(position)) {
            this.setKnownPosition(position);
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setKnownPosition(Position position) {
        position.getCharPos().ifPresent(c -> charPos = c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] b, int off, int len) {
        throw new UnsupportedOperationException("Call to read(byte[]) not permitted here");
    }

    /**
     * Consume buffered character
     *
     * @return consumed character
     */
    private char consume() {
        int c = nextChar;
        nextChar = NO_CHAR;
        charPos++;
        return (char) c;
    }

    /**
     * Read and fill character buffer, performing character conversion in the process.
     *
     * @param b   Buffer to fill
     * @param off Offset into buffer
     * @param len Length of characters to read (at least two)
     * @return Length read, or -1 if EOF reached
     * @throws IOException on IO Error
     */
    private int readImpl(char[] b, int off, int len) throws IOException {
        CharBuffer buffer = CharBuffer.wrap(b, off, len);
        if (buffer.position() != 0) {
            buffer = buffer.slice(); // makes position zero
        }
        boolean readEof = false;

        for (; ; ) {
            CoderResult coderResult = this.decoder.decode(this.bb, buffer, readEof);
            if (coderResult.isUnderflow()) {
                // Need more data to perform/finish conversion

                if (buffer.position() != 0 || readEof) {
                    // work with partial read or EOF
                    break;
                }

                // No data read, read next block from upstream
                int count = this.readBytes();
                if (count < 0) {
                    readEof = true;
                    if (!this.bb.hasRemaining()) {
                        // Nothing read, nothing in buffer
                        break;
                    }
                    this.decoder.reset();
                }
            } else {
                if (coderResult.isOverflow()) {
                    break;
                }
                // TODO: Need to convert to Prolog error
                coderResult.throwException();
            }
        }

        if (readEof) {
            this.decoder.reset();
            return IoUtility.EOF;
        }
        return buffer.position();
    }

    /**
     * Read next group of bytes (potentially next line) ready for conversion
     *
     * @return Number of bytes read
     * @throws IOException on IO Error
     */
    private int readBytes() throws IOException {
        this.bb.compact(); // may not be zero
        try {
            int limit = bb.limit();
            int pos = bb.position();
            int diff = pos <= limit ? limit - pos : 0;
            int count = super.read(this.bb.array(), this.bb.arrayOffset() + pos, diff);
            if (count < 0) {
                return count; // bb may contain some data from prior to readBytes
            }
            bb.position(pos + count);
        } finally {
            this.bb.flip();
        }
        return this.bb.remaining();
    }
}
