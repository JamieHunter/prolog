// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.bootstrap;

import prolog.constants.Atomic;
import prolog.io.IoBinding;
import prolog.io.PrologReadInteractiveStream;
import prolog.io.PrologWriteStdoutStream;
import prolog.library.Io;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Default set of IO bindings.
 */
public class DefaultIoBinding {

    /**
     * Well defined user input stream.
     */
    public static final IoBinding USER_INPUT = new IoBinding(Io.USER_INPUT_STREAM,
            PrologReadInteractiveStream.STREAM, null);
    /**
     * Well defined user output stream.
     */
    public static final IoBinding USER_OUTPUT = new IoBinding(Io.USER_OUTPUT_STREAM,
            null, PrologWriteStdoutStream.STREAM);


    private static final Map<Atomic, IoBinding> systemStreams = new HashMap<>();

    //
    // Well defined stream names.
    //
    static {
        systemStreams.put(USER_INPUT.getName(), USER_INPUT);
        systemStreams.put(USER_OUTPUT.getName(), USER_OUTPUT);
    }

    /**
     * Retrieve all well defined streams.
     *
     * @return Map of streams.
     */
    public static Map<? extends Atomic, ? extends IoBinding> getSystem() {
        return Collections.unmodifiableMap(systemStreams);
    }
}
