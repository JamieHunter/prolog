// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.io;

import org.jprolog.flags.CloseOptions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * Encode characters into multi-byte sequences. This is based on {@link sun.nio.cs.StreamEncoder}.
 */
public class OutputEncoderFilter extends FilteredOutputStream {
    private static final int NO_CHAR = -2;
    private static final int DEFAULT_BYTE_BUFFER_SIZE = 8192;
    private final CharsetEncoder encoder;
    private final ByteBuffer bb;
    private int nextChar = NO_CHAR;

    public OutputEncoderFilter(PrologOutputStream stream, Charset charset) {
        super(stream);
        encoder = charset.newEncoder().
                onMalformedInput(CodingErrorAction.REPLACE).
                onUnmappableCharacter(CodingErrorAction.REPLACE);
        this.bb = ByteBuffer.allocate(DEFAULT_BYTE_BUFFER_SIZE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(int chr) throws IOException {
        this.write(new char[]{(char) chr}, 0, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void write(char[] b, int off, int len) throws IOException {
        if (this.nextChar == IoUtility.EOF) {
            return;
        }
        CharBuffer buffer = CharBuffer.wrap(b, off, len);
        if (this.nextChar >= 0) {
            this.flushLeftoverChar(buffer, false);
        }

        while (buffer.hasRemaining()) {
            CoderResult codeResult = this.encoder.encode(buffer, this.bb, false);
            if (codeResult.isUnderflow()) {
                if (buffer.remaining() == 1) {
                    this.nextChar = buffer.get();
                }
                break;
            }

            if (codeResult.isOverflow()) {
                this.writeBytes();
            } else {
                codeResult.throwException();
            }
        }
        if (this.bb.position() > 0) {
            this.writeBytes();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(byte[] buffer, int off, int len) throws IOException {
        throw new UnsupportedOperationException("Call to write(byte[]) not permitted here");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void flush() throws IOException {
        if (nextChar == IoUtility.EOF) {
            return;
        }
        if (this.bb.position() > 0) {
            this.writeBytes();
        }
        super.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close(CloseOptions options) throws IOException {
        if (nextChar == IoUtility.EOF) {
            return; // already closed
        }

        this.flushLeftoverChar((CharBuffer) null, true);
        assert (nextChar == NO_CHAR);

        try {
            for (; ; ) {
                CoderResult coderResult = this.encoder.flush(this.bb);
                if (coderResult.isUnderflow()) {
                    if (this.bb.position() > 0) {
                        this.writeBytes();
                    }
                    super.close(options);
                    nextChar = IoUtility.EOF;
                    return;
                }

                if (coderResult.isOverflow()) {
                    assert this.bb.position() > 0;

                    this.writeBytes();
                } else {
                    // TODO: Convert to Prolog error
                    coderResult.throwException();
                }
            }
        } catch (IOException e) {
            this.encoder.reset();
            throw e;
        }
    }

    private void flushLeftoverChar(CharBuffer buffer, boolean closing) throws IOException {
        if (this.nextChar >= 0 || closing) {
            CharBuffer lcb = CharBuffer.allocate(2);
            lcb.clear();

            if (this.nextChar >= 0) {
                lcb.put((char) this.nextChar);
            }

            if (buffer != null && buffer.hasRemaining()) {
                lcb.put(buffer.get());
            }

            lcb.flip();

            while (lcb.hasRemaining() || closing) {
                CoderResult coderResult = this.encoder.encode(lcb, this.bb, closing);
                if (coderResult.isUnderflow()) {
                    if (lcb.hasRemaining()) {
                        this.nextChar = (char) lcb.get();
                        if (buffer != null && buffer.hasRemaining()) {
                            this.flushLeftoverChar(buffer, closing);
                        }

                        return;
                    }
                    break;
                }

                if (coderResult.isOverflow()) {
                    this.writeBytes();
                } else {
                    // TODO: convert to Prolog error
                    coderResult.throwException();
                }
            }

            this.nextChar = NO_CHAR;
        }
    }

    private void writeBytes() throws IOException {
        this.bb.flip();
        int limit = this.bb.limit();
        int pos = this.bb.position();

        int diff = pos <= limit ? limit - pos : 0;
        if (diff > 0) {
            super.write(this.bb.array(), this.bb.arrayOffset() + pos, diff);
        }
        this.bb.clear();
    }

}
