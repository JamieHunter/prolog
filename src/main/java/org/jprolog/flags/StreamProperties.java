// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.flags;

import org.jprolog.bootstrap.Interned;
import org.jprolog.constants.Atomic;
import org.jprolog.constants.PrologAtomInterned;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.CompoundTermImpl;
import org.jprolog.expressions.Term;
import org.jprolog.library.Io;
import org.jprolog.io.LogicalStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stream properties accessor.
 */
public class StreamProperties implements FlagsWithEnvironment {

    private static final ReadableParser<StreamProperties> parser = new ReadableParser<>();

    static {
        parser.atomFlag(Interned.internAtom("alias"), (o, v) -> o.environment().
                addStreamAlias((PrologAtomInterned)v, o.binding)).
                read(o -> o.binding.getAlias());
        parser.enumFlag(Interned.internAtom("buffer"), Buffering.class, (o, v) -> o.binding.setBufferMode(v)).
                readEnum(Buffering.class, o -> o.binding.getBufferMode());
        parser.intFlag(Interned.internAtom("buffer_size"), (o, v) -> o.binding.setBufferSize(v)).
                readInteger(o -> o.binding.getBufferSize());
        parser.protectedFlag(Interned.internAtom("bom")).
                readBoolean(o -> o.binding.isBomDetected());
        parser.booleanFlag(Interned.internAtom("close_on_abort"), (o, v) -> o.binding.setCloseOnAbort(v)).
                readBoolean(o -> o.binding.getCloseOnAbort());
        parser.booleanFlag(Interned.internAtom("close_on_exec"), (o, v) -> o.binding.setCloseOnExec(v)).
                readBoolean(o -> o.binding.getCloseOnExec());
        parser.enumFlag(Interned.internAtom("encoding"), Encoding.class, (o, v) -> o.binding.setEncoding(v)).
                readEnum(Encoding.class, o -> o.binding.getEncoding());
        parser.enumFlag(Interned.internAtom("eof_action"), EofAction.class, (o, v) -> o.binding.setEofAction(v)).
                readEnum(EofAction.class, o -> o.binding.getEofAction());
        parser.other(Interned.internAtom("file_name"), (o, v) -> o.binding.setObjectTerm(v)).
                read(o -> o.binding.getFileName());
        parser.protectedFlag(Interned.internAtom("input")).
                readBoolean(o -> o.binding.isInput());
        parser.intFlag(Interned.internAtom("line_position"), (o, v) -> o.binding.setLinePosition(v)).
                readInteger(o -> o.binding.getLinePosition());
        parser.protectedFlag(Interned.internAtom("mode")).
                readEnum(OpenMode.class, o -> o.binding.getOpenMode());
        parser.enumFlag(Interned.internAtom("newline"), NewLineMode.class, (o, v) -> o.binding.setNewLineMode(v)).
                readEnum(NewLineMode.class, o -> o.binding.getNewLineMode());
        parser.protectedFlag(Interned.internAtom("output")).
                readBoolean(o -> o.binding.isOutput());
        parser.protectedFlag(Interned.internAtom("position")).
                read(o -> o.binding.getPosition());
        parser.protectedFlag(Interned.internAtom("reposition")).
                readBoolean(o -> o.binding.canReposition());
        parser.enumFlag(Interned.internAtom("type"), Type.class, (o, v) -> o.binding.setType(v)).
                readEnum(Type.class, o -> o.binding.getType());
        parser.booleanFlag(Interned.internAtom("record_position"), (o, v) -> o.binding.setRecordPosition(v)).
                readBoolean(o -> o.binding.getRecordPosition());
        parser.booleanFlag(Interned.internAtom("tty"), (o, v) -> o.binding.setIsTTY(v)).
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
