// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import java.io.OutputStreamWriter;

/**
 * Singleton stream for writing to default output
 */
public class PrologWriteStdoutStream extends PrologWriteStreamImpl {

    /**
     * Singleton PrologWriteStream for STDOUT
     */
    public static final PrologWriteStream STREAM = new PrologWriteStdoutStream();

    private PrologWriteStdoutStream() {
        super("(stdout)", new OutputStreamWriter(System.out));
    }

    @Override
    public void close() {
        // no-op
    }
}
