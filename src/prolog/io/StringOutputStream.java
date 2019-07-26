// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Stream that writes to a string.
 */
public class StringOutputStream implements PrologOutputStream {

    private final StringWriter writer = new StringWriter();

    @Override
    public void write(int symbol) throws IOException {
        writer.write(symbol);
    }

    @Override
    public void write(String text) throws IOException {
        writer.write(text);
    }

    @Override
    public void write(char[] buffer, int off, int len) throws IOException {
        writer.write(buffer, off, len);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    @Override
    public String toString() {
        return writer.toString();
    }
}
