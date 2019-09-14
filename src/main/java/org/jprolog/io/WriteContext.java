// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.io;

import org.jprolog.execution.Environment;
import org.jprolog.flags.WriteOptions;

import java.io.IOException;

/**
 * State engine and other context for writing a term in a structured parsable way. In particular it manages a
 * state machine to identify when whitespace is required between tokens.
 */
public final class WriteContext {
    private final Environment environment;
    private final WriteOptions options;
    private final PrologOutputStream outputStream;
    private Safety safety = Safety.SAFE;

    /**
     * Create a context given an environment and a Prolog stream.
     *
     * @param environment  Execution environment.
     * @param options      Formatting options.
     * @param outputStream Output substream
     */
    public WriteContext(Environment environment, WriteOptions options, PrologOutputStream outputStream) {
        this.environment = environment;
        this.options = options;
        this.outputStream = outputStream;
    }

    /**
     * @return environment
     */
    public Environment environment() {
        return environment;
    }

    /**
     * @return options
     */
    public WriteOptions options() {
        return options;
    }

    /**
     * @return output stream
     */
    public PrologOutputStream output() {
        return outputStream;
    }

    /**
     * Write literal text
     *
     * @param text String to write
     * @throws IOException if IO error
     */
    public void write(String text) throws IOException {
        outputStream.write(text);
    }

    /**
     * Transition to an Alphanumeric token.
     *
     * @throws IOException if IO error
     */
    public void beginAlphaNum() throws IOException {
        if (safety.nextAlphaNum()) {
            writeSpace();
        }
        safety = Safety.ALPHANUMBER;
    }

    /**
     * Transition to a graphic token.
     *
     * @throws IOException if IO error
     */
    public void beginGraphic() throws IOException {
        if (safety.nextGraphic()) {
            writeSpace();
        }
        safety = Safety.GRAPHIC;
    }

    /**
     * Transition to a quoted token.
     *
     * @throws IOException if IO error
     */
    public void beginQuoted() throws IOException {
        if (safety.nextQuoted()) {
            writeSpace();
        }
        safety = Safety.QUOTED;
    }

    /**
     * Transition to a safe single graphic character (eg brackets).
     */
    public void beginSafe() {
        safety = Safety.SAFE;
    }

    /**
     * Used with unquoted atoms
     */
    public void beginUnknown() throws IOException {
        if (safety.nextUnknown()) {
            writeSpace();
        }
        safety = Safety.WS;
    }

    /**
     * Unconditionally write a whitespace
     *
     * @throws IOException if IO error
     */
    public void writeSpace() throws IOException {
        write(" ");
        safety = Safety.SAFE;
    }

    /**
     * This state engine determines if additional white space is required between two terms. As an example, if string
     * is parsed, safety=QUOTED. If this is followed by another string (nextQuoted()) then a white space is required
     * else it is not required.
     */
    private enum Safety {
        /**
         * State indicates that whitespace is never required
         */
        SAFE {
        },
        /**
         * State indicates that whitespace is always required (this is why all default implementations call ws()).
         */
        WS {
            @Override
            boolean ws() {
                return true;
            }
        },
        /**
         * State indicates an alphanumeric token was written. Space only required if followed by another alphanumeric
         * token.
         */
        ALPHANUMBER {
            @Override
            boolean nextAlphaNum() {
                return true;
            }
        },
        /**
         * State indicates a graphic token was written. Space only required if followed by another graphic token.
         */
        GRAPHIC {
            @Override
            boolean nextGraphic() {
                return true;
            }
        },
        /**
         * State indicates a quoted term was written (any variation). Space only required if followed by another
         * quoted term.
         */
        QUOTED {
            @Override
            boolean nextQuoted() {
                return true;
            }
        },;

        /**
         * @return true if whitespace is always required
         */
        boolean ws() {
            return false;
        }

        /**
         * Next token is alphanumeric
         *
         * @return true if whitespace is required
         */
        boolean nextAlphaNum() {
            return ws();
        }

        /**
         * Next token is graphic
         *
         * @return true if whitespace is required
         */
        boolean nextGraphic() {
            return ws();
        }

        /**
         * Next token is quoted
         *
         * @return true if whitespace is required
         */
        boolean nextQuoted() {
            return ws();
        }

        /**
         * Next token is unknown
         *
         * @return true if whitespace is required
         */
        boolean nextUnknown() {
            return ws();
        }
    }

}
