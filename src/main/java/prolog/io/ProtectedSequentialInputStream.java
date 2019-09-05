// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import prolog.flags.CloseOptions;

import java.io.IOException;
import java.io.InputStream;

/**
 * SequentialInputStream that cannot be closed
 */
public class ProtectedSequentialInputStream extends SequentialInputStream {

    public ProtectedSequentialInputStream(InputStream stream) {
        super(stream);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean approveClose(CloseOptions options) {
        return options.force;
    }
}
