// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Interned;
import prolog.bootstrap.Predicate;
import prolog.constants.Atomic;
import prolog.constants.PrologAtom;
import prolog.constants.PrologInteger;
import prolog.constants.PrologString;
import prolog.exceptions.PrologDomainError;
import prolog.exceptions.PrologError;
import prolog.exceptions.PrologExistenceError;
import prolog.exceptions.PrologInstantiationError;
import prolog.exceptions.PrologPermissionError;
import prolog.exceptions.PrologTypeError;
import prolog.execution.Backtrack;
import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.flags.OpenOptions;
import prolog.flags.ReadOptions;
import prolog.flags.WriteOptions;
import prolog.io.IoBinding;
import prolog.io.PrologReadInteractiveStream;
import prolog.io.PrologReadStream;
import prolog.io.PrologReadStreamImpl;
import prolog.io.PrologWriteStdoutStream;
import prolog.io.PrologWriteStream;
import prolog.io.PrologWriteStreamImpl;
import prolog.io.StructureWriter;
import prolog.io.WriteContext;
import prolog.unification.Unifier;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Bootstraps IO related predicates.
 */
public final class Io {
    private Io() {
        // Static methods/fields only
    }

    public static final PrologAtom USER_INPUT_STREAM = Interned.internAtom("user_input");
    public static final PrologAtom USER_OUTPUT_STREAM = Interned.internAtom("user_output");
    public static final PrologAtom END_OF_FILE = Interned.internAtom("end_of_file");
    public static final PrologAtom OPEN_APPEND = Interned.internAtom("append");
    public static final PrologAtom OPEN_READ = Interned.internAtom("read");
    public static final PrologAtom OPEN_WRITE = Interned.internAtom("write");
    public static final PrologAtom OPEN_UPDATE = Interned.internAtom("update");

    /**
     * Open a stream
     *
     * @param environment Execution environment
     * @param fileName    File to be opened
     * @param mode        Mode of open
     * @param streamIdent Captures stream identifier
     */
    @Predicate("open")
    public static void open(Environment environment, Term fileName, Term mode, Term streamIdent) {
        openHelper(environment, null, fileName, mode, streamIdent);
    }

    /**
     * Open a stream with options
     *
     * @param environment Execution environment
     * @param fileName    File to be opened
     * @param mode        Mode of open
     * @param streamIdent Captures stream identifier
     * @param options     List of open options
     */
    @Predicate("open")
    public static void open(Environment environment, Term fileName, Term mode, Term streamIdent, Term options) {
        openHelper(environment, options, fileName, mode, streamIdent);
    }

    /**
     * Change current input stream
     *
     * @param environment Execution environment
     * @param streamIdent Stream identifier
     */
    @Predicate("set_input")
    public static void setInput(Environment environment, Term streamIdent) {
        IoBinding binding = lookupStream(environment, streamIdent);
        PrologReadStream reader = getReader(environment, streamIdent, binding);
        IoBinding oldBinding = environment.setReader(binding);
        environment.pushBacktrack(new RestoreReader(environment, oldBinding));
    }

    /**
     * Retrieves current input stream
     *
     * @param environment Execution environment
     * @param streamIdent Receives stream identifier
     */
    @Predicate("current_input")
    public static void currentInput(Environment environment, Term streamIdent) {
        IoBinding readerBinding = environment.getReader();
        Unifier.unify(environment.getLocalContext(), streamIdent, readerBinding.getName());
    }

    /**
     * Change current output stream
     *
     * @param environment Execution environment
     * @param streamIdent Stream identifier
     */
    @Predicate("set_output")
    public static void setOutput(Environment environment, Term streamIdent) {
        IoBinding binding = lookupStream(environment, streamIdent);
        PrologWriteStream writer = getWriter(environment, streamIdent, binding);
        if (binding == null) {
            throw PrologDomainError.streamOrAlias(environment, streamIdent);
        }
        if (binding.getWrite() == null) {
            throw PrologPermissionError.error(environment, "output", "stream", streamIdent,
                    "Stream is not opened for writing");
        }
        IoBinding oldBinding = environment.setWriter(binding);
        environment.pushBacktrack(new RestoreWriter(environment, oldBinding));
    }

    /**
     * Retrieves current output stream
     *
     * @param environment Execution environment
     * @param streamIdent Receives stream identifier
     */
    @Predicate("current_output")
    public static void currentOutput(Environment environment, Term streamIdent) {
        IoBinding writerBinding = environment.getWriter();
        Unifier.unify(environment.getLocalContext(), streamIdent, writerBinding.getName());
    }

    /**
     * Closes specified stream
     *
     * @param environment Execution environment
     * @param streamIdent Stream identifier
     */
    @Predicate("close")
    public static void close(Environment environment, Term streamIdent) {
        IoBinding binding = lookupStream(environment, streamIdent);
        if (binding == null) {
            return;
        }
        environment.addStream(binding.getName(), null);
        environment.pushBacktrack(new RestoreStreamBinding(environment, binding.getName(), binding));
        try {
            binding.close();
        } catch (IOException ioe) {
            throw closeError(ioe, environment);
        }
    }

    /**
     * Get single character from current input stream
     *
     * @param environment Execution environment
     * @param term        Receives read character
     */
    @Predicate("get_char")
    public static void getChar(Environment environment, Term term) {
        IoBinding io = environment.getReader();
        PrologReadStream stream = io.getRead();
        Atomic value = stream.getChar(environment);
        Unifier.unify(environment.getLocalContext(), term, value);
    }

    /**
     * Writes single character to current output stream
     *
     * @param environment Execution environment
     * @param term        character to write
     */
    @Predicate("put_char")
    public static void putChar(Environment environment, Term term) {
        if (!term.isInstantiated()) {
            environment.backtrack();
            return;
        }
        String text;
        if (term.isInteger()) {
            text = String.valueOf((char) PrologInteger.from(term).get().intValue());
        } else if (term.isAtom()) {
            text = PrologAtom.from(term).get();
        } else {
            throw PrologTypeError.characterExpected(environment, term);
        }
        IoBinding io = environment.getWriter();
        PrologWriteStream stream = io.getWrite();
        try {
            stream.write(text);
        } catch (IOException ioe) {
            throw writeError(ioe, environment);
        }
    }

    /**
     * Reads a term from current input stream
     *
     * @param environment Execution environment
     * @param term        receives read term
     */
    @Predicate("read")
    public static void read(Environment environment, Term term) {
        IoBinding io = environment.getReader();
        PrologReadStream stream = io.getRead();
        Term value = stream.read(environment, new ReadOptions(environment, null));
        Unifier.unify(environment.getLocalContext(), term, value);
    }

    /**
     * Reads a term from specified input stream
     *
     * @param environment Execution environment
     * @param streamIdent Stream to read
     * @param term        receives read term
     */
    @Predicate("read")
    public static void read(Environment environment, Term streamIdent, Term term) {
        PrologReadStream stream = getReader(environment, streamIdent, null);
        Term value = stream.read(environment, new ReadOptions(environment, null));
        Unifier.unify(environment.getLocalContext(), term, value);
    }

    /**
     * Reads a term from specified input stream with processing options
     *
     * @param environment Execution environment
     * @param term        receives read term
     * @param options     Set of read options
     */
    @Predicate("read_term")
    public static void readTerm(Environment environment, Term term, Term options) {
        IoBinding io = environment.getReader();
        PrologReadStream stream = io.getRead();
        Term value = stream.read(environment, new ReadOptions(environment, options));
        Unifier.unify(environment.getLocalContext(), term, value);
    }

    /**
     * Reads a term from specified input stream with processing options
     *
     * @param environment Execution environment
     * @param streamIdent Stream to read
     * @param term        receives read term
     * @param options     Set of read options
     */
    @Predicate("read_term")
    public static void readTerm(Environment environment, Term streamIdent, Term term, Term options) {
        PrologReadStream stream = getReader(environment, streamIdent, null);
        Term value = stream.read(environment, new ReadOptions(environment, options));
        Unifier.unify(environment.getLocalContext(), term, value);
    }

    /**
     * Writes a term to current output stream
     *
     * @param environment Execution environment
     * @param term        term to write
     */
    @Predicate("write")
    public static void write(Environment environment, Term term) {
        IoBinding io = environment.getWriter();
        PrologWriteStream stream = io.getWrite();
        write(environment, null, stream, term);
    }

    /**
     * Writes a term to specified output stream
     *
     * @param environment Execution environment
     * @param streamIdent Stream to write
     * @param term        term to write
     */
    @Predicate("write")
    public static void write(Environment environment, Term streamIdent, Term term) {
        PrologWriteStream stream = getWriter(environment, streamIdent, null);
        write(environment, null, stream, term);
    }

    /**
     * Writes a term to current output stream with options
     *
     * @param environment Execution environment
     * @param term        term to write
     * @param options     formatting options
     */
    @Predicate("write_term")
    public static void writeTerm(Environment environment, Term term, Term options) {
        IoBinding io = environment.getWriter();
        PrologWriteStream stream = io.getWrite();
        write(environment, options, stream, term);
    }

    /**
     * Writes a term to specified output stream with options
     *
     * @param environment Execution environment
     * @param streamIdent Stream to write
     * @param term        term to write
     * @param options     formatting options
     */
    @Predicate("write_term")
    public static void writeTerm(Environment environment, Term streamIdent, Term term, Term options) {
        PrologWriteStream stream = getWriter(environment, streamIdent, null);
        write(environment, options, stream, term);
    }

    /**
     * Writes a new line to current output stream
     *
     * @param environment Execution environment
     */
    @Predicate("nl")
    public static void nl(Environment environment) {
        IoBinding io = environment.getWriter();
        PrologWriteStream stream = io.getWrite();
        try {
            stream.write("\n");
        } catch (IOException ioe) {
            throw writeError(ioe, environment);
        }
    }

    /**
     * Writes a new line to specified output stream
     *
     * @param environment Execution environment
     * @param streamIdent Stream to write
     */
    @Predicate("nl")
    public static void nl(Environment environment, Term streamIdent) {
        PrologWriteStream stream = getWriter(environment, streamIdent, null);
        try {
            stream.write("\n");
        } catch (IOException ioe) {
            throw writeError(ioe, environment);
        }
    }

    // ====================================================================
    // Helper methods
    // ====================================================================

    private static final PrologAtom OPEN_ACTION = Interned.internAtom("open");

    /**
     * Translates an open error
     *
     * @param cause       IO exception
     * @param environment Execution environment
     * @param fileName    File being opened
     * @return error to throw
     */
    public static PrologError openError(IOException cause, Environment environment, Term fileName) {
        if (cause instanceof FileNotFoundException) {
            return PrologExistenceError.error(environment,
                    Interned.SOURCE_SINK_DOMAIN, fileName, "File not found", cause);
        } else {
            return PrologPermissionError.error(environment,
                    OPEN_ACTION, Interned.SOURCE_SINK_DOMAIN, fileName, "Cannot open file", cause);
        }
    }

    /**
     * Translates a read error
     *
     * @param cause       IO exception
     * @param environment Execution environment
     * @return error to throw
     */
    public static PrologError readError(Throwable cause, Environment environment) {
        return PrologError.systemError(environment, cause);
    }

    /**
     * Translates a write error
     *
     * @param cause       IO exception
     * @param environment Execution environment
     * @return error to throw
     */
    public static PrologError writeError(Throwable cause, Environment environment) {
        return PrologError.systemError(environment, cause);
    }

    /**
     * Translates a close error
     *
     * @param cause       IO exception
     * @param environment Execution environment
     * @return error to throw
     */
    public static PrologError closeError(Throwable cause, Environment environment) {
        return PrologError.systemError(environment, cause);
    }

    /**
     * Open file, returning a binding
     *
     * @param environment Execution environment
     * @param optionsTerm Open options
     * @param fileName    File to be opened
     * @param mode        Mode to open file
     * @param streamAlias Desired stream alias
     * @return IoBinding
     */
    public static IoBinding openHelper(Environment environment, Term optionsTerm, Term fileName, Term mode, Term streamAlias) {
        OpenOptions options = new OpenOptions(environment, optionsTerm);
        // TODO: consider options above for the open mode below
        OpenOption[] op;
        if (mode == OPEN_READ) {
            op = new OpenOption[0];
        } else if (mode == OPEN_WRITE) {
            op = new OpenOption[]{StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING};
        } else if (mode == OPEN_UPDATE) {
            op = new OpenOption[]{StandardOpenOption.WRITE, StandardOpenOption.CREATE};
        } else if (mode == OPEN_APPEND) {
            op = new OpenOption[]{StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND};
        } else {
            throw PrologDomainError.ioMode(environment, mode);
        }

        Path path = parsePath(environment, fileName);
        Atomic name = null;
        if (streamAlias.isInstantiated()) {
            if (!(streamAlias.isAtom())) {
                throw PrologTypeError.atomExpected(environment, streamAlias);
            }
            name = (Atomic) streamAlias;
        }

        IoBinding binding;
        PrologReadStream reader = null;
        PrologWriteStream writer = null;

        try {
            if (op.length == 0) {
                if (path == null) {
                    reader = PrologReadInteractiveStream.STREAM;
                } else {
                    reader = new PrologReadStreamImpl(path);
                }
            } else {
                if (path == null) {
                    writer = PrologWriteStdoutStream.STREAM;
                } else {
                    writer = new PrologWriteStreamImpl(path, op);
                }
            }
        } catch (IOException ioe) {
            throw openError(ioe, environment, fileName);
        }
        if (name == null) {
            name = IoBinding.unique();
            Unifier.unify(environment.getLocalContext(), streamAlias, name);
        }
        binding = new IoBinding(name, reader, writer);
        IoBinding oldBinding = environment.addStream(name, binding);
        environment.pushBacktrack(new RestoreStreamBinding(environment, name, oldBinding));
        return binding;
    }

    /**
     * Convert a path parameter into an actual Path taking into account environment CWD.
     *
     * @param environment Execution environment
     * @param fileName    Term representing file name
     * @return Path to open
     */
    private static Path parsePath(Environment environment, Term fileName) {
        String pathName;
        if (fileName.isAtom()) {
            pathName = ((PrologAtom) (fileName.value(environment))).name();
        } else if (fileName.isString()) {
            pathName = ((PrologString) (fileName.value(environment))).get();
        } else {
            throw PrologDomainError.sourceSink(environment, fileName);
        }
        return environment.getCWD().resolve(Paths.get(pathName)).normalize();
    }

    /**
     * Helper, find stream by name
     *
     * @param streamIdent name of stream
     * @return stream via binding
     */
    public static IoBinding lookupStream(Environment environment, Term streamIdent) {
        if (!streamIdent.isInstantiated()) {
            throw PrologInstantiationError.error(environment, streamIdent);
        }
        if (!(streamIdent instanceof Atomic)) {
            throw PrologDomainError.streamOrAlias(environment, streamIdent);
        }
        IoBinding binding = environment.lookupStream((Atomic) streamIdent);
        if (binding == null) {
            throw PrologDomainError.streamOrAlias(environment, streamIdent);
        }
        return binding;
    }

    /**
     * Retrieve reader from environment.
     *
     * @param environment Execution environment
     * @param streamIdent Stream identifier
     * @param binding     Binding if known else null
     * @return Read stream
     */
    public static PrologReadStream getReader(Environment environment, Term streamIdent, IoBinding binding) {
        if (binding == null) {
            binding = lookupStream(environment, streamIdent);
        }
        if (binding.getRead() == null) {
            throw PrologPermissionError.error(environment, "input", "stream", streamIdent,
                    "Stream is not opened for reading");
        }
        return binding.getRead();
    }

    /**
     * Retrieve writer from environment.
     *
     * @param environment Execution environment
     * @param streamIdent Stream identifier
     * @param binding     Binding if known else null
     * @return Write stream
     */
    public static PrologWriteStream getWriter(Environment environment, Term streamIdent, IoBinding binding) {
        if (binding == null) {
            binding = lookupStream(environment, streamIdent);
        }
        if (binding.getWrite() == null) {
            throw PrologPermissionError.error(environment, "output", "stream", streamIdent,
                    "Stream is not opened for writing");
        }
        return binding.getWrite();
    }

    /**
     * Write term to stream
     *
     * @param environment Execution environment
     * @param optionsTerm Write options
     * @param stream      Stream to write to
     * @param term        Term to format and write
     */
    private static void write(Environment environment, Term optionsTerm, PrologWriteStream stream, Term term) {
        if (!term.isInstantiated()) {
            throw PrologInstantiationError.error(environment, term);
        }
        WriteOptions options = new WriteOptions(environment, optionsTerm);
        WriteContext context = new WriteContext(environment, options, stream);
        StructureWriter writer = new StructureWriter(context, term);
        try {
            writer.write();
        } catch (IOException ioe) {
            throw writeError(ioe, environment);
        }
    }

    /**
     * if the new binding has same name as old stream binding, the name is occluded.
     * This restores the occluded name on backtrack
     */
    private static class RestoreStreamBinding implements Backtrack {
        private final Environment environment;
        private final Atomic name;
        private final IoBinding binding;

        public RestoreStreamBinding(Environment environment, Atomic name, IoBinding binding) {
            this.environment = environment;
            this.name = name;
            this.binding = binding;
        }

        @Override
        public void undo() {
            environment.addStream(name, binding);
        }
    }

    /**
     * Restores the previous input stream
     */
    private static class RestoreReader implements Backtrack {
        private final Environment environment;
        private final IoBinding binding;

        public RestoreReader(Environment environment, IoBinding binding) {
            this.environment = environment;
            this.binding = binding;
        }

        @Override
        public void undo() {
            environment.setReader(binding);
        }
    }

    /**
     * Restores the previous output stream
     */
    private static class RestoreWriter implements Backtrack {
        private final Environment environment;
        private final IoBinding binding;

        public RestoreWriter(Environment environment, IoBinding binding) {
            this.environment = environment;
            this.binding = binding;
        }

        @Override
        public void undo() {
            environment.setWriter(binding);
        }
    }
}
