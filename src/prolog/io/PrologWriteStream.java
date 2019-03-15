// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import prolog.execution.Environment;
import prolog.expressions.Term;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;

/**
 * Interface for Prolog write streams.
 */
public interface PrologWriteStream extends Closeable {

    /**
     * @return Underlying Java writer
     */
    Writer javaWriter();

    /**
     * Write a string to output stream
     *
     * @param text Text to write
     * @throws IOException on IO Error
     */
    void write(String text) throws IOException;

    /**
     * Flush stream, making sure everything is written.
     *
     * @throws IOException on IO Error
     */
    void flush() throws IOException;
}
