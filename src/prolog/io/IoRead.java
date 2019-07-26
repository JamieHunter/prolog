// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import java.io.IOException;

/**
 * Single symbol read function allowing lambda usage.
 */
@FunctionalInterface
public interface IoRead {
    /**
     * Read a single symbol, -1 if EOF
     *
     * @throws IOException on IO Error
     */
    int read() throws IOException;
}
