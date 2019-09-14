// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.io;

import org.jprolog.flags.StreamProperties;

import java.io.IOException;

/**
 * Translate '\n' to end of line per newline mode
 */
public class OutputLineHandler extends FilteredOutputStream {
    private final char[] eoln;

    public OutputLineHandler(PrologOutputStream stream, StreamProperties.NewLineMode newlineMode) {
        super(stream);
        switch (newlineMode) {
            case ATOM_dos:
                eoln = new char[]{'\r', '\n'};
                break;
            case ATOM_posix:
                eoln = new char[]{'\n'};
                break;
            default:
                eoln = System.lineSeparator().toCharArray();
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(int b) throws IOException {
        if (b == '\n') {
            super.write(eoln, 0, eoln.length);
        } else {
            super.write(b);
        }
    }
}
