// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Represents interactive input (stdin)
 */
public class PrologReadInteractiveStream extends PrologReadStreamImpl {

    private static final StreamWrapper wrappedStdin = new StreamWrapper();

    /**
     * Singleton PrologReadString for interactive STDIN.
     */
    public static final PrologReadStream STREAM = new PrologReadInteractiveStream();

    private PrologReadInteractiveStream() {
        super("(interactive)", new BufferedReader(
                new InputStreamReader(wrappedStdin)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPrompt(Prompt prompt) {
        wrappedStdin.setPrompt(prompt);
    }

    /**
     * Line buffered input, presenting prompts when more data is required. This amount of work was required to allow
     * presenting output on each new line. This works well with BufferedReader.
     */
    private static class StreamWrapper extends InputStream {
        private Prompt prompt;
        private final byte[] buf = new byte[8192];
        private int start = 0;
        private int end = 0;
        private boolean seenEOF = false;

        /**
         * Read a single byte
         *
         * @return byte or -1 if EOF
         * @throws IOException on error
         */
        @Override
        public int read() throws IOException {
            if (!fillBuffer()) {
                return -1;
            }

            // Get next byte out of buffer
            return ((int) (buf[start++])) & 0xff;
        }

        /**
         * Read multiple bytes. Only through to end of line. BufferedReader will only call this method once as long
         * as some data was returned allowing line-by-line prompting.
         *
         * @param b   Target buffer
         * @param off Offset into target buffer
         * @param len Number of bytes to read
         * @return number of bytes read, or -1 if EOF
         * @throws IOException on error
         */
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (!fillBuffer()) {
                return -1;
            }
            if (len > (end - start)) {
                len = end - start;
            }
            for (int i = 0; i < len; i++) {
                b[off++] = buf[start++];
            }
            return len;
        }

        /**
         * Estimate of available bytes
         *
         * @return number of bytes
         * @throws IOException on error
         */
        @Override
        public int available() throws IOException {
            return (end - start) + System.in.available();
        }

        /**
         * Internal, fill buffer from stdin, pausing if '\n' reached
         *
         * @return true if buffer contains at least one character
         * @throws IOException on error
         */
        boolean fillBuffer() throws IOException {
            if (seenEOF) {
                return false;
            }
            if (start != end) {
                // Do not proceed to next line until this line is completely read
                return true;
            }
            if (System.in.available() == 0) {
                // Only need to prompt if read would be blocking
                doPrompt();
            }

            // Keep reading until end of line reached, then stop
            start = end = 0;
            while (end < buf.length) {
                int c = System.in.read();
                if (c < 0) {
                    seenEOF = true;
                    return end > 0;
                }
                buf[end++] = (byte) c;
                if (c == '\n') {
                    break;
                }
            }
            return true;
        }

        /**
         * Present a prompt.
         */
        void doPrompt() {
            String text = prompt.text();
            if (text != null) {
                System.err.print(prompt.text());
                System.err.flush();
                prompt = prompt.next();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            // ignored
        }

        /**
         * Change the prompt, making prompt available to the stream wrapper.
         *
         * @param prompt Prompt to change to.
         */
        void setPrompt(Prompt prompt) {
            this.prompt = prompt;
        }
    }
}
