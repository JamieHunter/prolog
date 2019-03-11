// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import java.io.BufferedReader;
import java.io.StringReader;

/**
 * Prolog read stream that is sourced by a string.
 */
public class PrologReadStringStream extends PrologReadStreamImpl {
    /**
     * Create stream from string
     *
     * @param text Text to parse
     */
    public PrologReadStringStream(String text) {
        super("(string)", new BufferedReader(new StringReader(text)));
    }
}
