// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import java.io.StringWriter;

/**
 * Prolog write stream that creates a string.
 */
public class PrologWriteStringStream extends PrologWriteStreamImpl {

    /**
     * Create stream from string
     *
     * @param stringWriter Target of stream
     */
    public PrologWriteStringStream(StringWriter stringWriter) {
        super("(string)", stringWriter);
    }
}
