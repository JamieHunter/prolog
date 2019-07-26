// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import prolog.flags.StreamProperties;

import java.io.IOException;

/**
 * Filter to handle line / prompt interaction
 */
public class InputLineHandler extends FilteredInputStream {
    private static final int NO_BYTE = -2;
    private final StreamProperties.NewLineMode newLineMode;
    private int nextByte = NO_BYTE;
    private State state = State.START_OF_LINE; // assume start of line

    /**
     * Create new filter
     *
     * @param stream      Stream being wrapped
     * @param newlineMode Specified newline mode
     */
    public InputLineHandler(PrologInputStream stream, StreamProperties.NewLineMode newlineMode) {
        super(stream);
        this.newLineMode = newlineMode;
    }

    /**
     * Change state, do not consume byte
     *
     * @param state New state
     */
    private void skip(State state) {
        this.state = state;
    }

    /**
     * Change state, do not consume symbol, return specified symbol.
     *
     * @param state New stare
     * @param b     Symbol to return
     * @return b
     */
    private int skip(State state, int b) {
        skip(state);
        return b;
    }

    /**
     * Change state, consume symbol, return consumed symbol
     *
     * @param state New state
     * @return consumed symbol
     */
    private int consume(State state) {
        return consume(state, this.nextByte);
    }

    /**
     * Change state, consume byte, return specified byte
     *
     * @param state New state
     * @param b     Byte to return
     * @return b
     */
    private int consume(State state, int b) {
        this.nextByte = NO_BYTE;
        this.state = state;
        return b;
    }

    private int nextByte() throws IOException {
        if (nextByte == NO_BYTE) {
            nextByte = super.read(); // assume correctness
        }
        return nextByte;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {
        for (; ; ) {
            switch (state) {
                case REGULAR:
                    // Regular parsing
                    if (nextByte() == IoUtility.EOF) {
                        return skip(State.EOF, IoUtility.EOF);
                    } else if (nextByte == '\r') {
                        if (newLineMode == StreamProperties.NewLineMode.ATOM_dos ||
                                newLineMode == StreamProperties.NewLineMode.ATOM_detect) {
                            consume(State.CR); // ignore the CR to look at next char
                            break; // look at next character
                        } else {
                            // ATOM_posix, '\r' is returned
                            return consume(State.REGULAR);
                        }
                    } else if (nextByte == '\n') {
                        // START_OF_LINE is always treated as START_OF_LINE regardless of mode
                        return consume(State.START_OF_LINE);
                    } else {
                        return consume(State.REGULAR);
                    }
                case CR:
                    // CR was skipped, need to look at next character
                    if (nextByte() == '\r') {
                        // CR CR
                        // CR now, and repeat
                        return consume(State.CR);
                    } else if (nextByte == '\n') {
                        // CR START_OF_LINE
                        // START_OF_LINE now, and prompt next
                        return consume(State.START_OF_LINE);
                    } else {
                        // CR <other>
                        // CR now, interpret next
                        return skip(State.REGULAR, '\r');
                    }
                case START_OF_LINE:
                    if (super.available() == 0) {
                        super.showPrompt();
                    }
                    state = State.REGULAR;
                    break;
                case EOF:
                    return skip(State.EOF, IoUtility.EOF);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(char[] b, int off, int len) throws IOException {
        if (state == State.EOF) {
            return IoUtility.EOF; // EOF reached
        }
        if (len <= 0) {
            return len;
        }
        for (int i = 0; i < len; i++) {
            int x = this.read(); // may prompt
            if (x == IoUtility.EOF) {
                return i > 0 ? i : IoUtility.EOF;
            }
            b[off++] = (char) i;
            if (state == State.START_OF_LINE) {
                return i; // don't fill past EOLN
            }
        }
        return len;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int available() throws IOException {
        if (nextByte == IoUtility.EOF) {
            return 0;
        } else {
            int size = super.available();
            return Math.max(size, (nextByte == NO_BYTE ? 0 : 1) + size);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        super.close();
        state = State.REGULAR;
        nextByte = NO_BYTE;
    }

    private enum State {
        REGULAR, // Regular parsing
        CR, // CR skipped, look at next char, CR may still be returned
        START_OF_LINE, // at start of line, prompt may need showing
        EOF, // EOF (-1) has been returned
    }

}
