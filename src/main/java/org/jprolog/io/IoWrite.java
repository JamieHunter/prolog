// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.io;

import java.io.IOException;

/**
 * Single symbol write function allowing lambda usage.
 */
@FunctionalInterface
public interface IoWrite {

    /**
     * Write a single symbol (byte or char).
     *
     * @param symbol Symbol to write
     * @throws IOException on IO Error
     */
    void write(int symbol) throws IOException;
}
