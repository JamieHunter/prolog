// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.bootstrap;

import prolog.constants.PrologAtomInterned;
import prolog.constants.PrologInteger;
import prolog.flags.StreamProperties;
import prolog.io.LogicalStream;
import prolog.io.ProtectedSequentialInputStream;
import prolog.io.ProtectedSequentialOutputStream;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Default set of IO bindings.
 */
public class DefaultIoBinding {

    /**
     * Name of well defined input stream
     */
    public static final PrologAtomInterned USER_INPUT_STREAM = Interned.internAtom("user_input");
    /**
     * Name of well defined output stream
     */
    public static final PrologAtomInterned USER_OUTPUT_STREAM = Interned.internAtom("user_output");
    /**
     * Name of well defined error output stream
     */
    public static final PrologAtomInterned USER_ERROR_STREAM = Interned.internAtom("user_error");

    /**
     * Well defined user input stream.
     */
    public static final LogicalStream USER_INPUT = new LogicalStream(
            PrologInteger.from(BigInteger.ZERO),
            new ProtectedSequentialInputStream(System.in),
            null,
            StreamProperties.OpenMode.ATOM_read
    );
    /**
     * Well defined user output stream.
     */
    public static final LogicalStream USER_OUTPUT = new LogicalStream(
            PrologInteger.from(1),
            null,
            new ProtectedSequentialOutputStream(System.out),
            StreamProperties.OpenMode.ATOM_write
    );
    /**
     * Well defined user error output stream.
     */
    public static final LogicalStream USER_ERROR = new LogicalStream(
            PrologInteger.from(2),
            null,
            new ProtectedSequentialOutputStream(System.err),
            StreamProperties.OpenMode.ATOM_write);

    private static final Map<PrologInteger, LogicalStream> initialStreams = new HashMap<>();
    private static final Map<PrologAtomInterned, LogicalStream> initialAliases = new HashMap<>();

    //
    // Well defined streams.
    //
    static {
        USER_INPUT.addAlias(USER_INPUT_STREAM);
        USER_INPUT.setObjectTerm(USER_INPUT.getId());
        USER_INPUT.setBufferMode(StreamProperties.Buffering.ATOM_line);
        USER_OUTPUT.addAlias(USER_OUTPUT_STREAM);
        USER_OUTPUT.setObjectTerm(USER_OUTPUT.getId());
        USER_INPUT.setBufferMode(StreamProperties.Buffering.ATOM_line);
        USER_ERROR.addAlias(USER_ERROR_STREAM);
        USER_ERROR.setObjectTerm(USER_ERROR.getId());
        USER_INPUT.setBufferMode(StreamProperties.Buffering.ATOM_false);
        StreamProperties.NewLineMode newlineMode = StreamProperties.NewLineMode.ATOM_detect;
        for (LogicalStream stream : new LogicalStream[]{USER_INPUT, USER_OUTPUT, USER_ERROR}) {
            stream.setIsTTY(true);
            stream.setType(StreamProperties.Type.ATOM_text);
            stream.setEncoding(StreamProperties.Encoding.ATOM_utf8);
            stream.setNewLineMode(newlineMode);
            stream.setCloseOnExec(false);
            stream.setCloseOnAbort(false);
            initialStreams.put(stream.getId(), stream);
            initialAliases.put((PrologAtomInterned) stream.getAlias(), stream);
        }
    }

    /**
     * Retrieve all well defined streams.
     *
     * @return Map of streams by ID.
     */
    public static Map<? extends PrologInteger, ? extends LogicalStream> getById() {
        return Collections.unmodifiableMap(initialStreams);
    }

    /**
     * Retrieve all well defined streams.
     *
     * @return Map of streams by name.
     */
    public static Map<? extends PrologAtomInterned, ? extends LogicalStream> getByAlias() {
        return Collections.unmodifiableMap(initialAliases);
    }
}
