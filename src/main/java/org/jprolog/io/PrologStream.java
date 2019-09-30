// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.io;

import org.jprolog.flags.CloseOptions;

import java.io.IOException;

/**
 * Common operations for both input and output streams.
 */
public interface PrologStream {

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
    default boolean seekPosition(Position position) throws IOException {
        return false;
    }

    /**
     * Restore virtual position (where applicable)
     *
     * @param position known position information
     */
    default void setKnownPosition(Position position) {
    }

    /**
     * Approve close
     * @param options Options passed into close
     * @return true if close permitted
     */
    default boolean approveClose(CloseOptions options) {
        return true;
    }

    /**
     * Close is attempted with flags.
     * @param options Options passed into close
     */
    void close(CloseOptions options) throws IOException;

    /**
     * Seek to end of stream if supported.
     */
    void seekEndOfStream() throws IOException;
}
