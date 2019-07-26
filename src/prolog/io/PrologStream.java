// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import java.io.Closeable;
import java.io.IOException;

/**
 * Common operations for both input and output streams.
 */
public interface PrologStream extends Closeable {

    /**
     * Fill position structure with known position information
     *
     * @param position known position information
     * @throws IOException on IO Error
     */
    default void getPosition(Position position) throws IOException {
    }

    /**
     * Seek to position (absolute)
     *
     * @param position target position information
     * @return true if position set
     * @throws IOException on IO Error
     */
    default boolean restorePosition(Position position) throws IOException {
        return false;
    }
}
