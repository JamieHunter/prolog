// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.DefaultIoBinding;
import prolog.bootstrap.Interned;
import prolog.bootstrap.Predicate;
import prolog.constants.Atomic;
import prolog.constants.PrologAtomInterned;
import prolog.constants.PrologAtomLike;
import prolog.constants.PrologEmptyList;
import prolog.constants.PrologFloat;
import prolog.constants.PrologInteger;
import prolog.constants.PrologString;
import prolog.exceptions.FutureFlagError;
import prolog.exceptions.PrologDomainError;
import prolog.exceptions.PrologError;
import prolog.exceptions.PrologExistenceError;
import prolog.exceptions.PrologInstantiationError;
import prolog.exceptions.PrologPermissionError;
import prolog.exceptions.PrologTypeError;
import prolog.execution.Backtrack;
import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.expressions.TermList;
import prolog.expressions.TermListImpl;
import prolog.flags.AbsoluteFileNameOptions;
import prolog.flags.CloseOptions;
import prolog.flags.OpenOptions;
import prolog.flags.StreamProperties;
import prolog.flags.WriteOptions;
import prolog.io.FileReadWriteStreams;
import prolog.io.LogicalStream;
import prolog.io.PrologInputStream;
import prolog.io.PrologOutputStream;
import prolog.unification.Unifier;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
    public static final PrologAtomInterned INPUT_ACTION = Interned.internAtom("input");
    public static final PrologAtomInterned OUTPUT_ACTION = Interned.internAtom("output");
    public static final PrologAtomInterned TEXT_STREAM = Interned.internAtom("text_stream");
    public static final PrologAtomInterned BINARY_STREAM = Interned.internAtom("binary_stream");
    public static final PrologAtomInterned BYTE_POS = Interned.internAtom("byte");
    public static final PrologAtomInterned CHAR_POS = Interned.internAtom("char");
    public static final PrologAtomInterned COLUMN_POS = Interned.internAtom("column");
    public static final PrologAtomInterned LINE_POS = Interned.internAtom("line");

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
        if (streamIdent.isInstantiated() && !streamIdent.isInteger()) {
            throw PrologDomainError.stream(environment, streamIdent);
        }
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
        if (streamIdent.isInstantiated() && !streamIdent.isInteger()) {
            throw PrologDomainError.stream(environment, streamIdent);
        }
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
        close(environment, streamIdent, PrologEmptyList.EMPTY_LIST);
    }

    /**
     * Closes specified stream with options
     *
     * @param environment Execution environment
     * @param streamIdent Stream identifier
     * @param options     Close options
     */
    @Predicate("close")
    public static void close(Environment environment, Term streamIdent, Term options) {
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        try {
            if (!logicalStream.close(new CloseOptions(environment, options))) {
                return;
            }
        } catch (IOException ioe) {
            throw closeError(ioe, environment);
        }
        LogicalStream inputStream = environment.getInputStream();
        LogicalStream outputStream = environment.getOutputStream();
        if (logicalStream == inputStream) {
            environment.setInputStream(DefaultIoBinding.USER_INPUT);
        }
        if (logicalStream == outputStream) {
            environment.setOutputStream(DefaultIoBinding.USER_OUTPUT);
        }
        environment.removeStream(logicalStream.getId(), logicalStream);
        ArrayList<PrologAtomInterned> aliases = logicalStream.getAliases();
        for (PrologAtomInterned alias : aliases) {
            environment.removeStreamAlias(alias, logicalStream);
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
        if (streamIdent.isInstantiated() && !streamIdent.isAtomic()) {
            throw PrologDomainError.stream(environment, streamIdent);
        }
        forEachStream(environment, streamIdent, (id, stream) -> {
            forEachStreamProperty(environment, id, stream, propertyStruct);
        });
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
        StreamProperties properties = new StreamProperties(environment, logicalStream, (Atomic) streamIdent);
        properties.set(propertyName, propertyValue);
    }

    /**
     * Enumerate all streams for matching streams
     *
     * @param environment Execution environment
     * @param object      Object path of stream
     * @param mode        read/write mode of stream
     * @param streamIdent stream identifier
     */
    @Predicate("current_stream")
    public static void currentStream(Environment environment, Term object, Term mode, Term streamIdent) {
        forEachStream(environment, streamIdent, (id, stream) -> {
            if (!(Unifier.unify(environment.getLocalContext(), object, stream.getFileName()) &&
                    Unifier.unify(environment.getLocalContext(), mode, stream.getMode()))) {
                environment.backtrack();
            }
        });
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
     * @param streamIdent Stream to modify
     * @param position    Opaque position
     */
    @Predicate("set_stream_position")
    public static void setStreamPosition(Environment environment, Term streamIdent, Term position) {
        // Restore position on a stream
        if (!position.isInstantiated()) {
            throw PrologInstantiationError.error(environment, position);
        }
        // TODO: end_of_stream(at)
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        logicalStream.restorePosition(environment, (Atomic) streamIdent, position);
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
     * Get single character from current input stream
     *
     * @param environment Execution environment
     * @param streamIdent Stream to get character from
     * @param chr         Character retrieved
     */
    @Predicate("get_char")
    public static void getChar(Environment environment, Term streamIdent, Term chr) {
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        Atomic value = logicalStream.getChar(environment, (Atomic) streamIdent);
        if (!Unifier.unify(environment.getLocalContext(), chr, value)) {
            environment.backtrack();
        }
    }

    /**
     * Get single character from current input stream
     *
     * @param environment Execution environment
     * @param term        Receives read code
     */
    @Predicate("get_code")
    public static void getCode(Environment environment, Term term) {
        LogicalStream logicalStream = environment.getInputStream();
        Atomic value = logicalStream.getCode(environment, null);
        if (!Unifier.unify(environment.getLocalContext(), term, value)) {
            environment.backtrack();
        }
    }

    /**
     * Get single character code from current input stream
     *
     * @param environment Execution environment
     * @param streamIdent Stream to get character from
     * @param code        Character retrieved
     */
    @Predicate("get_code")
    public static void getCode(Environment environment, Term streamIdent, Term code) {
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        Atomic value = logicalStream.getCode(environment, (Atomic) streamIdent);
        if (!Unifier.unify(environment.getLocalContext(), code, value)) {
            environment.backtrack();
        }
    }

    /**
     * Get single character from current input stream
     *
     * @param environment Execution environment
     * @param term        Receives read code
     */
    @Predicate("get_byte")
    public static void getByte(Environment environment, Term term) {
        LogicalStream logicalStream = environment.getInputStream();
        Atomic value = logicalStream.getByte(environment, null);
        if (!Unifier.unify(environment.getLocalContext(), term, value)) {
            environment.backtrack();
        }
    }

    /**
     * Get single byte code from current input stream
     *
     * @param environment Execution environment
     * @param streamIdent Stream to get character from
     * @param code        Character retrieved
     */
    @Predicate("get_byte")
    public static void getByte(Environment environment, Term streamIdent, Term code) {
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        Atomic value = logicalStream.getByte(environment, (Atomic) streamIdent);
        if (!Unifier.unify(environment.getLocalContext(), code, value)) {
            environment.backtrack();
        }
    }

    /**
     * Writes single character to current output stream
     *
     * @param environment Execution environment
     * @param term        character to write
     */
    @Predicate({"put_char", "put_code"})
    public static void putChar(Environment environment, Term term) {
        LogicalStream logicalStream = environment.getOutputStream();
        logicalStream.putChar(environment, null, term);
    }

    /**
     * Writes single character to specified output stream
     *
     * @param environment Execution environment
     * @param streamIdent Stream to get character from
     * @param term        character to write
     */
    @Predicate({"put_char", "put_code"})
    public static void putChar(Environment environment, Term streamIdent, Term term) {
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        logicalStream.putChar(environment, (Atomic) streamIdent, term);
    }

    /**
     * Writes single byte to current output stream
     *
     * @param environment Execution environment
     * @param term        character to write
     */
    @Predicate("put_byte")
    public static void putByte(Environment environment, Term term) {
        LogicalStream logicalStream = environment.getOutputStream();
        logicalStream.putByte(environment, null, term);
    }

    /**
     * Writes single byte to specified output stream
     *
     * @param environment Execution environment
     * @param streamIdent Stream to get character from
     * @param term        character to write
     */
    @Predicate("put_byte")
    public static void putByte(Environment environment, Term streamIdent, Term term) {
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        logicalStream.putByte(environment, (Atomic) streamIdent, term);
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
        WriteOptions options = new WriteOptions(environment, null);
        options.numbervars = true;
        options.quoted = false;
        options.ignoreOps = false;
        logicalStream.write(environment, null, term, options);
    }

    /**
     * Writes a term, quoted, to current output stream
     *
     * @param environment Execution environment
     * @param term        term to write
     */
    @Predicate("writeq")
    public static void writeq(Environment environment, Term term) {
        LogicalStream logicalStream = environment.getOutputStream();
        WriteOptions options = new WriteOptions(environment, null);
        options.numbervars = true;
        options.quoted = true;
        options.ignoreOps = false;
        logicalStream.write(environment, null, term, options);
    }

    /**
     * Writes a term, in canonical form, to current output stream
     *
     * @param environment Execution environment
     * @param term        term to write
     */
    @Predicate("write_canonical")
    public static void writeCanonical(Environment environment, Term term) {
        LogicalStream logicalStream = environment.getOutputStream();
        WriteOptions options = new WriteOptions(environment, null);
        options.numbervars = false;
        options.quoted = true;
        options.ignoreOps = true;
        logicalStream.write(environment, null, term, options);
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
        WriteOptions options = new WriteOptions(environment, null);
        options.numbervars = true;
        options.quoted = false;
        options.ignoreOps = false;
        logicalStream.write(environment, null, term, options);
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
        WriteOptions options = new WriteOptions(environment, null);
        options.numbervars = true;
        options.quoted = false;
        options.ignoreOps = false;
        logicalStream.write(environment, (Atomic) streamIdent, term, options);
    }

    /**
     * Writes a term, quoted, to current output stream
     *
     * @param environment Execution environment
     * @param streamIdent Stream to write
     * @param term        term to write
     */
    @Predicate("writeq")
    public static void writeq(Environment environment, Term streamIdent, Term term) {
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        WriteOptions options = new WriteOptions(environment, null);
        options.numbervars = true;
        options.quoted = true;
        options.ignoreOps = false;
        logicalStream.write(environment, null, term, options);
    }

    /**
     * Writes a term, in canonical form, to current output stream
     *
     * @param environment Execution environment
     * @param streamIdent Stream to write
     * @param term        term to write
     */
    @Predicate("write_canonical")
    public static void writeCanonical(Environment environment, Term streamIdent, Term term) {
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        WriteOptions options = new WriteOptions(environment, null);
        options.numbervars = false;
        options.quoted = true;
        options.ignoreOps = true;
        logicalStream.write(environment, null, term, options);
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
        WriteOptions options = new WriteOptions(environment, null);
        options.numbervars = true;
        options.quoted = false;
        options.ignoreOps = false;
        logicalStream.write(environment, (Atomic) streamIdent, term, options);
        logicalStream.nl(environment, null);
    }

    /**
     * Writes a term to current output stream with options
     *
     * @param environment Execution environment
     * @param term        term to write
     * @param optionsTerm formatting options
     */
    @Predicate("write_term")
    public static void writeTerm(Environment environment, Term term, Term optionsTerm) {
        LogicalStream logicalStream = environment.getOutputStream();
        WriteOptions options = new WriteOptions(environment, optionsTerm);
        options.numbervars = true;
        logicalStream.write(environment, null, term, options);
    }

    /**
     * Writes a term to specified output stream with options
     *
     * @param environment Execution environment
     * @param streamIdent Stream to write
     * @param term        term to write
     * @param optionsTerm formatting options
     */
    @Predicate("write_term")
    public static void writeTerm(Environment environment, Term streamIdent, Term term, Term optionsTerm) {
        LogicalStream logicalStream = lookupStream(environment, streamIdent);
        WriteOptions options = new WriteOptions(environment, optionsTerm);
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
     *
     * @param environment Execution environment
     * @param fileName    Name of file
     * @param timeTerm    Unified with time of file
     */
    @Predicate("time_file")
    public static void timeFile(Environment environment, Term fileName, Term timeTerm) {
        Path path = parsePathWithCWD(environment, fileName);
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
        if (cause instanceof FileNotFoundException || cause instanceof NoSuchFileException) {
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

        if (!fileName.isInstantiated()) {
            throw PrologInstantiationError.error(environment, fileName);
        }
        if (!mode.isInstantiated()) {
            throw PrologInstantiationError.error(environment, fileName);
        }
        mode = PrologAtomInterned.from(environment, mode);
        // TODO: consider options above for the open mode below
        Set<OpenOption> op = new HashSet<OpenOption>();
        if (mode.is(OPEN_READ)) {
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

        Path path = parsePathWithCWD(environment, fileName);
        PrologAtomInterned aliasName = null;
        if (streamTarget.isInstantiated()) {
            if (!(streamTarget.isAtom())) {
                throw PrologTypeError.atomExpected(environment, streamTarget);
            }
            aliasName = PrologAtomInterned.from(environment, streamTarget);
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
        binding.setObjectTerm(fileName);
        binding.setIsTTY(false);
        environment.addStream(binding.getId(), binding);
        // TODO: BOM

        // Two different ways of specifying an alias
        if (aliasName != null) {
            environment.addStreamAlias(aliasName, binding);
        }
        options.alias.ifPresent(prologAtom ->
                environment.addStreamAlias(PrologAtomInterned.from(environment, prologAtom), binding));
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
     * Convert a path parameter into an actual Path, taking into account environment CWD. No searching is performed.
     *
     * @param environment Execution environment
     * @param fileName    Term representing file name
     * @return Path to open
     */
    public static Path parsePathWithCWD(Environment environment, Term fileName) {
        Path basic = parsePathBasic(environment, fileName);
        if (basic.isAbsolute()) {
            return basic.normalize();
        }
        return environment.getCWD().resolve(basic).normalize();
    }

    /**
     * Enumerate path with alternative substitutions. A transform function is called for each potential path to
     * accept (return actual path) or reject (return null).
     *
     * @param environment Execution environment
     * @param fileName    Term representing file name
     * @param transform   Validate and transform search path into actual path.
     * @return Path to open, or null if path not validated via transform function
     */
    public static Path parsePathSearch(Environment environment, Term fileName, Function<Path,Path> transform) {
        Path basic = parsePathBasic(environment, fileName);
        if (basic.isAbsolute()) {
            basic = transform.apply(basic);
            if (basic != null) {
                basic = basic.normalize();
            }
            return basic;
        }
        ListIterator<Path> search = environment.getSearchPath().listIterator();
        while (search.hasNext()) {
            Path path = search.next().resolve(basic);
            path = transform.apply(path);
            if (path != null) {
                return path.normalize();
            }
        }
        Path finalPath = transform.apply(environment.getCWD().resolve(basic));
        if (finalPath != null) {
            finalPath = finalPath.normalize();
        }
        return finalPath;
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
            throw PrologExistenceError.stream(environment, streamIdent);
        }
        return logicalStream;
    }

    /**
     * @param environment Execution environment
     * @param fileSpec    File to convert
     * @param options     Options to use for conversion
     * @return path in absolute form
     */
    public static Path absoluteFileName(Environment environment, Term fileSpec, AbsoluteFileNameOptions options) {
        if (CompoundTerm.termIsA(fileSpec, Interned.LIBRARY_FUNCTOR, 1)) {
            throw new UnsupportedOperationException("library(X) not yet handled");
        }

        Path usePath = parsePathSearch(environment, fileSpec, path -> {
            for (String ext : options.extensions) {
                Path test = path.getParent().resolve(path.getFileName().toString() + ext);
                try {
                    BasicFileAttributes attribs = Files.readAttributes(test, BasicFileAttributes.class);
                    if (options.type == AbsoluteFileNameOptions.FileType.ATOM_directory) {

                        if (attribs.isDirectory()) {
                            return test.toRealPath();
                        }
                    } else {
                        if (attribs.isRegularFile()) {
                            return test.toRealPath();
                        }
                    }
                } catch (IOException e) {
                    // ignore this test
                }
            }
            return null;
        });
        if (usePath == null) {
            usePath = parsePathWithCWD(environment, fileSpec);
            try {
                usePath = usePath.toRealPath();
            } catch (IOException e) {
                // Ignore?
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

    /**
     * Many stream accessors operate on either a given stream, or all streams
     *
     * @param environment Execution environment
     * @param streamIdent Stream identifier - Variable or Atomic
     * @param lambda      Operation to perform on the stream
     */
    private static void forEachStream(Environment environment, Term streamIdent, BiConsumer<Atomic, LogicalStream> lambda) {
        if (streamIdent.isInstantiated()) {
            LogicalStream stream = lookupStream(environment, streamIdent);
            lambda.accept((Atomic) streamIdent, stream);
        } else {
            List<LogicalStream> all = new ArrayList<>();
            all.addAll(environment.getOpenStreams());
            new ForEachStream(environment, all.listIterator(), streamIdent, lambda).next();
        }
    }

    private static class ForEachStream extends DecisionPoint {
        private final ListIterator<LogicalStream> iter;
        private final Term streamIdent;
        private final BiConsumer<Atomic, LogicalStream> lambda;

        private ForEachStream(Environment environment, ListIterator<LogicalStream> iter, Term streamIdent, BiConsumer<Atomic, LogicalStream> lambda) {
            super(environment);
            this.iter = iter;
            this.streamIdent = streamIdent;
            this.lambda = lambda;
        }

        @Override
        protected void next() {
            if (!iter.hasNext()) {
                environment.backtrack();
                return;
            }
            LogicalStream stream = iter.next();
            environment.forward();
            if (iter.hasNext()) {
                environment.pushDecisionPoint(this);
            }
            if (!Unifier.unify(environment.getLocalContext(), streamIdent, stream.getId())) {
                environment.backtrack();
                return;
            }
            lambda.accept(stream.getId(), stream);
        }
    }

    /**
     * Enumerate single property or all properties of a stream
     *
     * @param environment Execution environment
     * @param streamIdent Stream identifier (Atomic)
     * @param stream      Logical stream
     * @param property    Stream property to be bound
     */
    private static void forEachStreamProperty(Environment environment, Atomic streamIdent, LogicalStream stream, Term property) {
        if (property.isInstantiated()) {
            if (property instanceof CompoundTerm) {
                CompoundTerm compoundProperty = (CompoundTerm) property;
                if (compoundProperty.arity() == 1) {
                    Atomic propertyName = compoundProperty.functor();
                    Term unifyValue = compoundProperty.get(0);
                    StreamProperties properties = new StreamProperties(environment, stream, streamIdent);
                    try {
                        Term actualValue = properties.get(propertyName);
                        if (!Unifier.unify(environment.getLocalContext(), unifyValue, actualValue)) {
                            environment.backtrack();
                        }
                        return;
                    } catch (FutureFlagError ffe) {
                        // handled below
                    }
                }
            } else if (property.isAtom()) {
                if (property.compareTo(INPUT_ACTION) == 0) {
                    if (!stream.isInput()) {
                        environment.backtrack();
                    }
                    return;
                } else if (property.compareTo(OUTPUT_ACTION) == 0) {
                    if (!stream.isOutput()) {
                        environment.backtrack();
                    }
                    return;
                }
            }
            throw PrologDomainError.streamProperty(environment, property);
        } else {
            List<Term> allProps = new StreamProperties(environment, stream, streamIdent).getAll(stream);
            new ForEachStreamProperty(environment, allProps.iterator(), property).next();
        }
    }

    private static class ForEachStreamProperty extends DecisionPoint {
        private final Iterator<Term> iter;
        private final Term property;

        private ForEachStreamProperty(Environment environment, Iterator<Term> iter, Term property) {
            super(environment);
            this.iter = iter;
            this.property = property;
        }

        @Override
        protected void next() {
            if (!iter.hasNext()) {
                environment.backtrack();
                return;
            }
            Term term = iter.next();
            environment.forward();
            if (iter.hasNext()) {
                environment.pushDecisionPoint(this);
            }
            if (!Unifier.unify(environment.getLocalContext(), property, term)) {
                environment.backtrack();
            }
        }
    }
}
