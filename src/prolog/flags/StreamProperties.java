// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.flags;

import prolog.constants.Atomic;
import prolog.execution.Environment;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;
import prolog.io.LogicalStream;
import prolog.library.Io;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static prolog.bootstrap.Interned.internAtom;

/**
 * Stream properties accessor.
 */
public class StreamProperties implements FlagsWithEnvironment {

    private static final ReadableParser<StreamProperties> parser = new ReadableParser<>();

    static {
        parser.atomFlag(internAtom("alias"), (o, v) -> o.environment().addStreamAlias(v, o.binding)).
                read(o -> o.binding.getAlias());
        parser.enumFlag(internAtom("buffer"), Buffering.class, (o, v) -> o.binding.setBufferMode(v)).
                readEnum(Buffering.class, o -> o.binding.getBufferMode());
        parser.intFlag(internAtom("buffer_size"), (o, v) -> o.binding.setBufferSize(v)).
                readInteger(o -> o.binding.getBufferSize());
        parser.protectedFlag(internAtom("bom")).
                readBoolean(o -> o.binding.isBomDetected());
        parser.booleanFlag(internAtom("close_on_abort"), (o, v) -> o.binding.setCloseOnAbort(v)).
                readBoolean(o -> o.binding.getCloseOnAbort());
        parser.booleanFlag(internAtom("close_on_exec"), (o, v) -> o.binding.setCloseOnExec(v)).
                readBoolean(o -> o.binding.getCloseOnExec());
        parser.enumFlag(internAtom("encoding"), Encoding.class, (o, v) -> o.binding.setEncoding(v)).
                readEnum(Encoding.class, o -> o.binding.getEncoding());
        parser.enumFlag(internAtom("eof_action"), EofAction.class, (o, v) -> o.binding.setEofAction(v)).
                readEnum(EofAction.class, o -> o.binding.getEofAction());
        parser.other(internAtom("file_name"), (o, v) -> o.binding.setObjectTerm(v)).
                read(o -> o.binding.getFileName());
        parser.protectedFlag(internAtom("input")).
                readBoolean(o -> o.binding.isInput());
        parser.intFlag(internAtom("line_position"), (o, v) -> o.binding.setLinePosition(v)).
                readInteger(o -> o.binding.getLinePosition());
        parser.protectedFlag(internAtom("mode")).
                readEnum(OpenMode.class, o -> o.binding.getOpenMode());
        parser.enumFlag(internAtom("newline"), NewLineMode.class, (o, v) -> o.binding.setNewLineMode(v)).
                readEnum(NewLineMode.class, o -> o.binding.getNewLineMode());
        parser.protectedFlag(internAtom("output")).
                readBoolean(o -> o.binding.isOutput());
        parser.protectedFlag(internAtom("position")).
                read(o -> o.binding.getPosition());
        parser.protectedFlag(internAtom("reposition")).
                readBoolean(o -> o.binding.canReposition());
        parser.enumFlag(internAtom("type"), Type.class, (o, v) -> o.binding.setType(v)).
                readEnum(Type.class, o -> o.binding.getType());
        parser.booleanFlag(internAtom("record_position"), (o, v) -> o.binding.setRecordPosition(v)).
                readBoolean(o -> o.binding.getRecordPosition());
        parser.booleanFlag(internAtom("tty"), (o, v) -> o.binding.setIsTTY(v)).
                readBoolean(o -> o.binding.getIsTTY());
//      locale, timeout, representation_errors, write_errors
    }

    private final Environment environment;
    private final LogicalStream binding;
    private final Atomic streamIdent;

    /**
     * Create a new PrologFlags associated with a new environment
     *
     * @param binding Prolog stream
     */
    public StreamProperties(Environment environment, LogicalStream binding, Atomic streamIdent) {
        this.environment = environment;
        this.binding = binding;
        this.streamIdent = streamIdent;
    }

    @Override
    public Environment environment() {
        return environment;
    }

    public LogicalStream getBinding() {
        return binding;
    }

    /**
     * Retrieve specified property for this stream
     *
     * @param key Property key
     * @return Property value
     */
    public Term get(Atomic key) {
        return parser.get(this, key);
    }

    /**
     * Retrieve all properties for this stream
     *
     * @return map of all properties
     */
    public List<Term> getAll(LogicalStream stream) {
        List<Term> terms = new ArrayList<>();
        if (stream.isInput()) {
            terms.add(Io.INPUT_ACTION);
        }
        if (stream.isOutput()) {
            terms.add(Io.OUTPUT_ACTION);
        }
        for(Map.Entry<Atomic, Term> e : parser.getAll(this).entrySet()) {
            terms.add(new CompoundTermImpl(e.getKey(), e.getValue()));
        }
        return terms;
    }

    /**
     * Modified specified property for this stream
     *
     * @param key   Property key
     * @param value Property value
     */
    public void set(Atomic key, Term value) {
        parser.set(this, key, value);
    }

    public enum Buffering {
        ATOM_full,
        ATOM_line,
        ATOM_false
    }

    public enum NewLineMode {
        ATOM_detect,
        ATOM_posix,
        ATOM_dos
    }

    public enum OpenMode {
        ATOM_read,
        ATOM_write,
        ATOM_append,
        ATOM_update
    }

    public enum Create {
        ATOM_read,
        ATOM_write,
        ATOM_execute,
        ATOM_default,
        ATOM_all
    }

    public enum Encoding {
        ATOM_utf8,
        ATOM_octet,
        ATOM_ascii,
        ATOM_iso_latin_1,
        //ATOM_text,
        ATOM_unicode_be,
        ATOM_unicode_le
    }

    public enum EofAction {
        ATOM_eof_code,
        ATOM_error,
        ATOM_reset
    }

    public enum Type {
        ATOM_text,
        ATOM_binary
    }
}
