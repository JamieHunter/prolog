// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import java.io.Closeable;
import java.io.IOException;

/**
 * Base class for Prolog streams.
 */
public abstract class PrologStream implements Closeable {
    private final String sourceSinkName;

    /**
     * Stream with associated source or sink name.
     *
     * @param sourceSinkName Name of file or other source/sink indicator
     */
    public PrologStream(String sourceSinkName) {
        this.sourceSinkName = sourceSinkName;
    }

    /**
     * Retrieve source/sink name associated with this stream.
     * @return name
     */
    public String getSourceSinkName() {
        return sourceSinkName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
    }
}
