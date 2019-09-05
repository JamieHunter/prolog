// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import prolog.exceptions.FutureFlagPermissionError;
import prolog.flags.CloseOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * SequentialOutputStream that cannot be closed
 */
public class ProtectedSequentialOutputStream extends SequentialOutputStream {

    public ProtectedSequentialOutputStream(OutputStream stream) {
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
