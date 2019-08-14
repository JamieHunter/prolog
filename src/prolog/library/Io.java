// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Interned;
import prolog.bootstrap.Predicate;
import prolog.constants.Atomic;
import prolog.constants.PrologAtomInterned;
import prolog.constants.PrologAtomLike;
import prolog.constants.PrologEmptyList;
import prolog.constants.PrologFloat;
import prolog.constants.PrologInteger;
import prolog.constants.PrologString;
import prolog.exceptions.PrologDomainError;
import prolog.exceptions.PrologError;
import prolog.exceptions.PrologExistenceError;
import prolog.exceptions.PrologInstantiationError;
import prolog.exceptions.PrologPermissionError;
import prolog.exceptions.PrologTypeError;
import prolog.execution.Backtrack;
import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.expressions.TermList;
import prolog.expressions.TermListImpl;
import prolog.flags.AbsoluteFileNameOptions;
import prolog.flags.OpenOptions;
import prolog.flags.StreamProperties;
import prolog.io.FileReadWriteStreams;
import prolog.io.LogicalStream;
import prolog.io.PrologInputStream;
import prolog.io.PrologOutputStream;
import prolog.unification.Unifier;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Bootstraps IO related predicates.
 */
public final class Io {
    private Io() {
        // Static methods/fields only
    }

    // Universal EOF atom
    public static final PrologAtomInterned END_OF_FILE = Interned.internAtom("end_of_file");
    // Stream modes
    public static final PrologAtomInterned OPEN_APPEND = Interned.internAtom("append");
    public static final PrologAtomInterned OPEN_READ = Interned.internAtom("read");
    public static final PrologAtomInterned OPEN_WRITE = Interned.internAtom("write");
    public static final PrologAtomInterned OPEN_UPDATE = Interned.internAtom("update");

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
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        // verify and prepare stream, return value ignored
        logicalStream.getInputStream(environment, (Atomic) streamIdent);
        LogicalStream oldInputStream = environment.setInputStream(logicalStream);
        // TODO, this may be wrong
        environment.pushBacktrack(new RestoreInput(environment, oldInputStream));
    }

    /**
     * Retrieves current input stream
     *
     * @param environment Execution environment
     * @param streamIdent Receives stream identifier
     */
    @Predicate("current_input")
    public static void currentInput(Environment environment, Term streamIdent) {
        LogicalStream currentInputStream = environment.getInputStream();
        if (!Unifier.unify(environment.getLocalContext(), streamIdent, currentInputStream.getId())) {
            environment.backtrack();
        }
    }

    /**
     * Change current output stream
     *
     * @param environment Execution environment
     * @param streamIdent Stream identifier
     */
    @Predicate("set_output")
    public static void setOutput(Environment environment, Term streamIdent) {
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        // verify and prepare stream, return value ignored
        logicalStream.getOutputStream(environment, (Atomic) streamIdent);
        LogicalStream oldOutputStream = environment.setOutputStream(logicalStream);
        // TODO, this may be wrong
        environment.pushBacktrack(new RestoreOutput(environment, oldOutputStream));
    }

    /**
     * Retrieves current output stream
     *
     * @param environment Execution environment
     * @param streamIdent Receives stream identifier
     */
    @Predicate("current_output")
    public static void currentOutput(Environment environment, Term streamIdent) {
        LogicalStream currentOutputStream = environment.getOutputStream();
        if (!Unifier.unify(environment.getLocalContext(), streamIdent, currentOutputStream.getId())) {
            environment.backtrack();
        }
    }

    /**
     * Closes specified stream
     *
     * @param environment Execution environment
     * @param streamIdent Stream identifier
     */
    @Predicate("close")
    public static void close(Environment environment, Term streamIdent) {
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        // Remove only from primary stream table
        // TODO: Do we remove aliases too?
        environment.addStream(logicalStream.getId(), null);
        try {
            logicalStream.close();
        } catch (IOException ioe) {
            throw closeError(ioe, environment);
        }
    }

    /**
     * Query one of the streams properties.
     *
     * @param environment    Execution Environment
     * @param streamIdent    Stream to query property from
     * @param propertyStruct A structure indicating requested property. Structure need not be grounded.
     */
    @Predicate("stream_property")
    public static void streamProperty(Environment environment, Term streamIdent, Term propertyStruct) {
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        // some possible optimization here if propertyStruct known at compile time
        if (!(propertyStruct instanceof CompoundTerm)) {
            throw PrologTypeError.compoundExpected(environment, propertyStruct);
        }
        CompoundTerm compoundProperty = (CompoundTerm) propertyStruct;
        if (compoundProperty.arity() != 1) {
            throw PrologDomainError.streamProperty(environment, compoundProperty);
        }
        Atomic propertyName = compoundProperty.functor();
        Term unifyValue = compoundProperty.get(0);
        StreamProperties properties = new StreamProperties(environment, logicalStream);
        Term actualValue = properties.get(propertyName);
        if (!Unifier.unify(environment.getLocalContext(), unifyValue, actualValue)) {
            environment.backtrack();
        }
    }

    /**
     * Modify a stream property.
     *
     * @param environment    Execution Environment
     * @param streamIdent    Stream to update property of
     * @param propertyStruct A structure indicating requested property. Structure is not grounded.
     */
    @Predicate("set_stream")
    public static void setStream(Environment environment, Term streamIdent, Term propertyStruct) {
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        // some possible optimization here if propertyStruct known at compile time
        if (!(propertyStruct instanceof CompoundTerm)) {
            throw PrologTypeError.compoundExpected(environment, propertyStruct);
        }
        CompoundTerm compoundProperty = (CompoundTerm) propertyStruct;
        if (compoundProperty.arity() != 1) {
            throw PrologDomainError.streamProperty(environment, compoundProperty);
        }
        Atomic propertyName = compoundProperty.functor();
        Term propertyValue = compoundProperty.get(0);
        StreamProperties properties = new StreamProperties(environment, logicalStream);
        properties.set(propertyName, propertyValue);
    }

    /**
     * Query some properties of stream, or enumerate all streams with properties of stream, or find matching stream.
     * That is, this operates over all streams.
     *
     * @param compiling Compiling context
     * @param term      Structure current_stream(Object, Mode, Stream)
     */
    @Predicate(value = "current_stream", arity = 3)
    public static void currentStream(CompileContext compiling, CompoundTerm term) {
        // E.g. current_stream(?Object, ?Mode, ?Stream)
        // Can be used to enumerate all streams
        throw new UnsupportedOperationException("NYI");
    }

    /**
     * Check stream exists - backtrack if it does not.
     *
     * @param environment Execution Environment
     * @param streamIdent Stream to query property from
     */
    @Predicate("is_stream")
    public static void isStream(Environment environment, Term streamIdent) {
        LogicalStream logicalStream = lookupStreamUnsafe(environment, streamIdent);
        if (logicalStream == null) {
            environment.backtrack();
        }
    }

    /**
     * Restore absolute position of stream
     *
     * @param environment Execution environment
     * @param stream      Stream to modify
     * @param position    Opaque position
     */
    @Predicate("set_stream_position")
    public static void setStreamPosition(Environment environment, Term stream, Term position) {
        // Restore position on a stream
        throw new UnsupportedOperationException("NYI");
    }

    /**
     * Retrieve detailed positioning information of stream
     *
     * @param environment  Execution environment
     * @param positionTerm Opaque positioning term
     * @param method       Information desired
     * @param data         Position information
     */
    @Predicate("stream_position_data")
    public static void streamPositionData(Environment environment, Term positionTerm, Term method, Term data) {
        // Obtain position of a stream (compare with seek)
        throw new UnsupportedOperationException("NYI");
    }

    /**
     * Change position, allowing for relative positioning
     *
     * @param environment Execution environment
     * @param stream      Stream to reposition
     * @param offset      Desired position per method
     * @param method      Method to use to reposition
     * @param location    New absolute position
     */
    @Predicate("seek")
    public static void seek(Environment environment, Term stream, Term offset, Term method, Term location) {
        throw new UnsupportedOperationException("NYI");
    }

    /**
     * Get single character from current input stream
     *
     * @param environment Execution environment
     * @param term        Receives read character
     */
    @Predicate("get_char")
    public static void getChar(Environment environment, Term term) {
        LogicalStream logicalStream = environment.getInputStream();
        Atomic value = logicalStream.getChar(environment, null);
        if (!Unifier.unify(environment.getLocalContext(), term, value)) {
            environment.backtrack();
        }
    }

    /**
     * Writes single character to current output stream
     *
     * @param environment Execution environment
     * @param term        character to write
     */
    @Predicate("put_char")
    public static void putChar(Environment environment, Term term) {
        LogicalStream logicalStream = environment.getOutputStream();
        logicalStream.putChar(environment, null, term);
    }

    /**
     * Reads a term from current input stream
     *
     * @param environment Execution environment
     * @param term        receives read term
     */
    @Predicate("read")
    public static void read(Environment environment, Term term) {
        LogicalStream logicalStream = environment.getInputStream();
        logicalStream.read(environment, null, term, null);
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
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        logicalStream.read(environment, (Atomic) streamIdent, term, null);
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
        LogicalStream logicalStream = environment.getInputStream();
        logicalStream.read(environment, null, term, options);
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
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        logicalStream.read(environment, (Atomic) streamIdent, term, options);
    }

    /**
     * Writes a term to current output stream
     *
     * @param environment Execution environment
     * @param term        term to write
     */
    @Predicate("write")
    public static void write(Environment environment, Term term) {
        LogicalStream logicalStream = environment.getOutputStream();
        logicalStream.write(environment, null, term, null);
    }

    /**
     * Writes a term to current output stream
     *
     * @param environment Execution environment
     * @param term        term to write
     */
    @Predicate("writeln")
    public static void writeln(Environment environment, Term term) {
        LogicalStream logicalStream = environment.getOutputStream();
        logicalStream.write(environment, null, term, null);
        logicalStream.nl(environment, null);
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
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        logicalStream.write(environment, (Atomic) streamIdent, term, null);
    }

    /**
     * Writes a term to specified output stream
     *
     * @param environment Execution environment
     * @param streamIdent Stream to write
     * @param term        term to write
     */
    @Predicate("writeln")
    public static void writeln(Environment environment, Term streamIdent, Term term) {
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        logicalStream.write(environment, (Atomic) streamIdent, term, null);
        logicalStream.nl(environment, null);
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
        LogicalStream logicalStream = environment.getOutputStream();
        logicalStream.write(environment, null, term, options);
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
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        logicalStream.write(environment, (Atomic) streamIdent, term, options);
    }

    /**
     * Writes a new line to current output stream
     *
     * @param environment Execution environment
     */
    @Predicate("nl")
    public static void nl(Environment environment) {
        LogicalStream logicalStream = environment.getOutputStream();
        logicalStream.nl(environment, null);
    }

    /**
     * Writes a new line to specified output stream
     *
     * @param environment Execution environment
     * @param streamIdent Stream to write
     */
    @Predicate("nl")
    public static void nl(Environment environment, Term streamIdent) {
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        logicalStream.nl(environment, (Atomic) streamIdent);
    }

    /**
     * Retrieve last modification time of file
     * @param environment Execution environment
     * @param fileName Name of file
     * @param timeTerm Unified with time of file
     */
    @Predicate("time_file")
    public static void timeFile(Environment environment, Term fileName, Term timeTerm) {
        Path path = parsePath(environment, fileName);
        PrologFloat time = Time.toPrologTime(path.toFile().lastModified());
        if (!Unifier.unify(environment.getLocalContext(), timeTerm, time)) {
            environment.backtrack();
        }
    }

    @Predicate("absolute_file_name")
    public static void absoluteFileName(Environment environment, Term fileSpec, Term fileName) {
        Path path = absoluteFileName(environment, fileSpec, new AbsoluteFileNameOptions(environment, null));
        unifyFilePath(environment, path, fileName);
    }

    @Predicate("absolute_file_name")
    public static void absoluteFileName(Environment environment, Term fileSpec, Term fileName, Term options) {
        Path path = absoluteFileName(environment, fileSpec, new AbsoluteFileNameOptions(environment, options));
        unifyFilePath(environment, path, fileName);
    }

    @Predicate("expand_file_name")
    public static void expandFileName(Environment environment, Term fileSpec, Term expansion) {

        Path path = parsePathBasic(environment, fileSpec);
        // TODO: Interpret expansion meta-characters
        TermList expanded = new TermListImpl(Arrays.asList(fileSpec), PrologEmptyList.EMPTY_LIST);
        if (!Unifier.unify(environment.getLocalContext(), expansion, expanded)) {
            environment.backtrack();
        }
    }

    // ====================================================================
    // Helper methods
    // ====================================================================

    private static final PrologAtomInterned OPEN_ACTION = Interned.internAtom("open");

    /**
     * Translates an open error
     *
     * @param cause       IO exception
     * @param environment Execution environment
     * @param fileName    File being opened
     * @return error to throw
     */
    private static PrologError openError(IOException cause, Environment environment, Term fileName) {
        if (cause instanceof FileNotFoundException) {
            return PrologExistenceError.error(environment,
                    Interned.SOURCE_SINK_DOMAIN, fileName, "File not found", cause);
        } else {
            return PrologPermissionError.error(environment,
                    OPEN_ACTION, Interned.SOURCE_SINK_DOMAIN, fileName, "Cannot open file", cause);
        }
    }

    /**
     * Translates a close error
     *
     * @param cause       IO exception
     * @param environment Execution environment
     * @return error to throw
     */
    private static PrologError closeError(Throwable cause, Environment environment) {
        return PrologError.systemError(environment, cause);
    }

    /**
     * Open file, returning a stream
     *
     * @param environment  Execution environment
     * @param optionsTerm  Open options
     * @param fileName     File to be opened
     * @param mode         Mode to open file
     * @param streamTarget Variable, or desired stream alias
     */
    private static void openHelper(Environment environment, Term optionsTerm, Term fileName, Term mode, Term streamTarget) {
        OpenOptions options = new OpenOptions(environment, optionsTerm);
        StreamProperties.OpenMode openMode;
        // TODO: consider options above for the open mode below
        Set<OpenOption> op = new HashSet<OpenOption>();
        if (mode == OPEN_READ) {
            op.add(StandardOpenOption.READ);
            openMode = StreamProperties.OpenMode.ATOM_read;
        } else if (mode == OPEN_WRITE) {
            op.add(StandardOpenOption.WRITE);
            op.add(StandardOpenOption.CREATE);
            op.add(StandardOpenOption.TRUNCATE_EXISTING);
            openMode = StreamProperties.OpenMode.ATOM_write;
        } else if (mode == OPEN_UPDATE) {
            op.add(StandardOpenOption.WRITE);
            op.add(StandardOpenOption.CREATE);
            openMode = StreamProperties.OpenMode.ATOM_update;
        } else if (mode == OPEN_APPEND) {
            op.add(StandardOpenOption.WRITE);
            op.add(StandardOpenOption.CREATE);
            op.add(StandardOpenOption.APPEND);
            openMode = StreamProperties.OpenMode.ATOM_append;
        } else {
            throw PrologDomainError.ioMode(environment, mode);
        }

        OpenOption[] ops = op.toArray(new OpenOption[0]);

        Path path = parsePath(environment, fileName);
        PrologAtomLike aliasName = null;
        if (streamTarget.isInstantiated()) {
            if (!(streamTarget.isAtom())) {
                throw PrologTypeError.atomExpected(environment, streamTarget);
            }
            aliasName = (PrologAtomLike) streamTarget;
        }

        LogicalStream binding;
        FileReadWriteStreams fileStream;

        try {
            fileStream = new FileReadWriteStreams(FileChannel.open(path, ops));
        } catch (IOException ioe) {
            throw openError(ioe, environment, fileName);
        }
        PrologInputStream input = mode == OPEN_READ ? fileStream : null;
        PrologOutputStream output = mode != OPEN_READ ? fileStream : null;
        PrologInteger id = LogicalStream.unique();
        if (aliasName == null) {
            if (!Unifier.unify(environment.getLocalContext(), streamTarget, id)) {
                environment.backtrack();
                return;
            }
        }
        binding = new LogicalStream(id, input, output, openMode);
        binding.setBufferMode(options.buffer);
        binding.setType(options.type);
        binding.setCloseOnAbort(options.closeOnAbort);
        //binding.setCloseOnExec(options.closeOnExec);
        binding.setEncoding(options.encoding.orElse(options.type == StreamProperties.Type.ATOM_text
                ? StreamProperties.Encoding.ATOM_utf8 : StreamProperties.Encoding.ATOM_octet));
        binding.setEofAction(options.eofAction);
        binding.setFileName(fileName);
        binding.setIsTTY(false);
        environment.addStream(binding.getId(), binding);
        // TODO: BOM

        // Two different ways of specifying an alias
        if (aliasName != null) {
            environment.addStreamAlias(aliasName, binding);
        }
        options.alias.ifPresent(prologAtom -> environment.addStreamAlias(prologAtom, binding));
    }

    /**
     * Convert a path parameter into an actual Path taking into account environment CWD.
     *
     * @param environment Execution environment
     * @param fileName    Term representing file name
     * @return Path to open
     */
    private static Path parsePathBasic(Environment environment, Term fileName) {
        String pathName;
        if (fileName.isAtom()) {
            pathName = ((PrologAtomLike) (fileName.value(environment))).name();
        } else if (fileName.isString()) {
            pathName = ((PrologString) (fileName.value(environment))).get();
        } else {
            throw PrologDomainError.sourceSink(environment, fileName);
        }
        return Paths.get(pathName);
    }

    /**
     * Convert a path parameter into an actual Path, not taking into account environment CWD.
     *
     * @param environment Execution environment
     * @param fileName    Term representing file name
     * @return Path to open
     */
    private static Path parsePath(Environment environment, Term fileName) {
        Path basic = parsePathBasic(environment, fileName);
        return environment.getCWD().resolve(basic).normalize();
    }

    /**
     * Helper, find stream by name, may return null
     *
     * @param streamIdent name of stream
     * @return stream via binding or null if not found
     */
    private static LogicalStream lookupStreamUnsafe(Environment environment, Term streamIdent) {
        if (!streamIdent.isInstantiated()) {
            throw PrologInstantiationError.error(environment, streamIdent);
        }
        if (!(streamIdent instanceof Atomic)) {
            throw PrologDomainError.streamOrAlias(environment, streamIdent);
        }
        return environment.lookupStream((Atomic) streamIdent);
    }

    /**
     * Helper, find stream by name. Error thrown if stream not found
     *
     * @param streamIdent name of stream
     * @return stream via binding
     */
    public static LogicalStream lookupStream(Environment environment, Term streamIdent) {
        LogicalStream logicalStream = lookupStreamUnsafe(environment, streamIdent);
        if (logicalStream == null) {
            throw PrologDomainError.streamOrAlias(environment, streamIdent);
        }
        return logicalStream;
    }

    /**
     *
     * @param environment Execution environment
     * @param fileSpec File to convert
     * @param options Options to use for conversion
     * @return path in absolute form
     */
    public static Path absoluteFileName(Environment environment, Term fileSpec, AbsoluteFileNameOptions options) {
        if (CompoundTerm.termIsA(fileSpec, Interned.LIBRARY_FUNCTOR, 1)) {
            throw new UnsupportedOperationException("library(X) not yet handled");
        }
        Path path = parsePath(environment, fileSpec);
        Path usePath = null;
        for(String ext : options.extensions) {
            Path test = path.getParent().resolve(path.getFileName().toString() + ext);
            try {
                BasicFileAttributes attribs = Files.readAttributes(test, BasicFileAttributes.class);
                if (options.type == AbsoluteFileNameOptions.FileType.ATOM_directory) {

                    if (attribs.isDirectory()) {
                        usePath = test.toRealPath();
                        break;
                    }
                } else {
                    if (attribs.isRegularFile()) {
                        usePath = test.toRealPath();
                        break;
                    }
                }
            } catch (IOException e) {
                // ignore this test
            }
        }
        if (usePath == null) {
            try {
                usePath = path.toRealPath();
            } catch (IOException e) {
                // TODO: this can be improved
                usePath = path;
            }
        }
        return usePath;
    }

    public static boolean unifyFilePath(Environment environment, Path path, Term fileNameTerm) {
        PrologString fileName = new PrologString(path.toString());
        if (Unifier.unify(environment.getLocalContext(), fileNameTerm, fileName)) {
            return true;
        } else {
            environment.backtrack();
            return false;
        }
    }

    /**
     * Undoes an alias on backtrack
     */
    private static class RestoreStreamBinding implements Backtrack {
        private final Environment environment;
        private final PrologAtomInterned alias;
        private final LogicalStream binding;

        RestoreStreamBinding(Environment environment, PrologAtomInterned alias, LogicalStream binding) {
            this.environment = environment;
            this.alias = alias;
            this.binding = binding;
        }

        @Override
        public void undo() {
            environment.addStreamAlias(alias, binding);
        }
    }

    /**
     * Restores the previous input stream
     */
    private static class RestoreInput implements Backtrack {
        private final Environment environment;
        private final LogicalStream binding;

        RestoreInput(Environment environment, LogicalStream binding) {
            this.environment = environment;
            this.binding = binding;
        }

        @Override
        public void undo() {
            environment.setInputStream(binding);
        }
    }

    /**
     * Restores the previous output stream
     */
    private static class RestoreOutput implements Backtrack {
        private final Environment environment;
        private final LogicalStream binding;

        RestoreOutput(Environment environment, LogicalStream binding) {
            this.environment = environment;
            this.binding = binding;
        }

        @Override
        public void undo() {
            environment.setOutputStream(binding);
        }
    }
}
