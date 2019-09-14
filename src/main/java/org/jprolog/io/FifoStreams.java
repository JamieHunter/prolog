// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The FIFO stream will take blocks of writes, and replay them as blocks of reads. It is assumed to
 * be at the byte layer of operation. Implemented as Java IO allows us to reuse {@link SequentialInputStream}
 * and {@link SequentialOutputStream}.
 */
public class FifoStreams {

    private final FifoOutputStream output = new FifoOutputStream();
    private final FifoInputStream input = new FifoInputStream();
    private final Deque<byte[]> queue = new LinkedList<>();
    private final static int CHUNK_SIZE = 1024;
    private final AtomicBoolean outputClosed = new AtomicBoolean(false);
    private final Object lock = new Object();
    private long available = 0L; // protect with lock

    public FifoStreams() {
    }

    /**
     * Override to fill buffer with more data
     *
     * @param stream Stream to write to
     */
    public void onEmpty(OutputStream stream) {
        // called to fill stream on empty.
    }

    /**
     * @return Stream to read
     */
    public InputStream getInput() {
        return input;
    }

    /**
     * @return Stream to write
     */
    public OutputStream getOutput() {
        return output;
    }

    // Note, the intermediate buffer is probably not the most efficient way of doing this,
    // but sufficient for initial version.
    private class FifoOutputStream extends OutputStream {

        private ByteArrayOutputStream partial = null;

        private FifoOutputStream() {
        }

        /**
         * Sets up partial data buffer for write.
         */
        private void prepare() {
            if (partial == null) {
                partial = new ByteArrayOutputStream();
            }
        }

        /**
         * Adds data to FIFO as and when enough has been written.
         */
        private void chunk() {
            if (partial != null && partial.size() >= CHUNK_SIZE) {
                flush();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void flush() {
            synchronized (lock) {
                if (partial == null || partial.size() == 0) {
                    return;
                }
                available += partial.size();
                queue.addLast(partial.toByteArray());
                partial = null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(int b) {
            synchronized (lock) {
                prepare();
                partial.write(b);
                chunk();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(byte[] b, int off, int len) {
            if (len > 0) {
                synchronized (lock) {
                    prepare();
                    partial.write(b, off, len);
                    chunk();
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            synchronized (lock) {
                flush();
                outputClosed.set(true);
            }
        }
    }

    // Note, the intermediate buffer is probably not the most efficient way of doing this,
    // but sufficient for initial version.
    private class FifoInputStream extends InputStream {

        private ByteArrayInputStream partial = null;

        /**
         * Makes data from output stream available to input stream
         *
         * @return true if data is available
         */
        private boolean prepare() {
            if (partial == null || partial.available() == 0) {
                byte[] data = queue.pollFirst();
                if (data == null) {
                    // first, implicit flush
                    output.flush();
                    data = queue.poll();
                }
                if (data == null) {
                    // still no data available
                    partial = null;
                    return false;
                }
                partial = new ByteArrayInputStream(data);
            }
            return true;
        }

        /**
         * Make data from output stream available to input stream. If
         * no data available in input stream, call onEmpty callback.
         *
         * @return true if data is available
         */
        private boolean prepareAndFill() {
            boolean hasData = prepare();
            if (!hasData) {
                onEmpty(output);
                hasData = prepare();
            }
            return hasData;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int available() throws IOException {
            synchronized (lock) {
                return (int) Math.min(available, (long) Integer.MAX_VALUE);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read() throws IOException {
            synchronized (lock) {
                if (!prepareAndFill()) {
                    return IoUtility.EOF;
                }
                int symbol = partial.read();
                if (symbol >= 0) {
                    available--;
                }
                return symbol;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            synchronized (lock) {
                if (!prepareAndFill()) {
                    return IoUtility.EOF;
                }
                int counted = partial.read(b, off, len);
                if (counted >= 0) {
                    available -= counted;
                }
                return counted;
            }
        }
    }
}
