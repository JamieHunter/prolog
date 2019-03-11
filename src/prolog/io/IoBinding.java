// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import prolog.constants.Atomic;
import prolog.constants.PrologInteger;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Associates a constant with an input and output stream pair.
 */
public class IoBinding {

    // provides a unique identifier over lifetime of execution.
    private static final AtomicInteger counter = new AtomicInteger(1);

    /**
     * Create a unique stream ID
     *
     * @return new stream ID
     */
    public static Atomic unique() {
        int n = counter.getAndIncrement();
        return new PrologInteger(BigInteger.valueOf(n));
    }

    private final Atomic name;
    private final PrologReadStream read;
    private final PrologWriteStream write;

    /**
     * Construct a binding object.
     *
     * @param name  Name of "stream" (binding)
     * @param read  Read stream, or null
     * @param write Write stream, or null
     */
    public IoBinding(Atomic name, PrologReadStream read, PrologWriteStream write) {
        this.name = name;
        this.read = read;
        this.write = write;
    }

    /**
     * Close streams.
     *
     * @throws IOException IO Error
     */
    public void close() throws IOException {
        if (read != null) {
            read.close();
        }
        if (write != null) {
            write.close();
        }
    }

    /**
     * @return Name of stream
     */
    public Atomic getName() {
        return this.name;
    }

    /**
     * @return Reader
     */
    public PrologReadStream getRead() {
        return this.read;
    }

    /**
     * @return Writer
     */
    public PrologWriteStream getWrite() {
        return this.write;
    }
}
