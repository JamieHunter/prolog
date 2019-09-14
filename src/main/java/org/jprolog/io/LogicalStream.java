// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.io;

import org.jprolog.bootstrap.Interned;
import org.jprolog.constants.Atomic;
import org.jprolog.constants.PrologAtom;
import org.jprolog.constants.PrologAtomInterned;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.constants.PrologCharacter;
import org.jprolog.constants.PrologEmptyList;
import org.jprolog.constants.PrologInteger;
import org.jprolog.exceptions.FutureFlagKeyError;
import org.jprolog.exceptions.PrologDomainError;
import org.jprolog.exceptions.PrologError;
import org.jprolog.exceptions.PrologInstantiationError;
import org.jprolog.exceptions.PrologPermissionError;
import org.jprolog.exceptions.PrologTypeError;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.CompoundTermImpl;
import org.jprolog.expressions.Term;
import org.jprolog.expressions.TermList;
import org.jprolog.flags.CloseOptions;
import org.jprolog.flags.ReadOptions;
import org.jprolog.flags.StreamProperties;
import org.jprolog.flags.WriteOptions;
import org.jprolog.library.Io;
import org.jprolog.parser.ExpressionReader;
import org.jprolog.parser.Tokenizer;
import org.jprolog.unification.Unifier;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Associates a constant with an input and output substream pair. Generally only one is used, but this allows
 * for a read/write logical stream.
 */
public class LogicalStream {

    // provides a unique identifier over lifetime of execution.
    private static final AtomicInteger counter = new AtomicInteger(10000); // at least not 0,1 or 2
    public static final LogicalStream NONE = new LogicalStream();

    /**
     * Create a unique stream ID
     *
     * @return new stream ID
     */
    public static PrologInteger unique() {
        int n = counter.getAndIncrement();
        return PrologInteger.from(n);
    }

    private WeakHashMap<Environment, Integer> protection = null;
    public static final int PROTECT_INPUT = 1;
    public static final int PROTECT_OUTPUT = 2;
    public static final int PROTECT_ERROR = 4;
    private final PrologInteger id;
    private final PrologInputStream baseInput;
    private final PrologOutputStream baseOutput;
    private PrologInputStream input = null;
    private PrologOutputStream output = null;
    private PositionTracker tracker = null;
    private List<PrologAtomInterned> aliases = new ArrayList<>();
    private StreamProperties.OpenMode openMode;
    private StreamProperties.NewLineMode newLineMode = StreamProperties.NewLineMode.ATOM_detect;
    private StreamProperties.Buffering bufferMode = StreamProperties.Buffering.ATOM_full;
    private Long bufferSize = null;
    private boolean closeOnAbort = true;
    private boolean closeOnExec = true;
    private boolean closed = false;
    private StreamProperties.Encoding encoding = StreamProperties.Encoding.ATOM_utf8;
    private StreamProperties.EofAction eofAction = StreamProperties.EofAction.ATOM_error;
    private StreamProperties.Type type = StreamProperties.Type.ATOM_text;
    private Term fileName = PrologEmptyList.EMPTY_LIST;
    private boolean recordPosition = true;
    private boolean seekable = true;
    private boolean isTTY = false;
    private boolean ioChanged;

    /**
     * Construct a binding object.
     *
     * @param id       id of "stream"
     * @param input    Base Input stream, usually a facade around Java stream.
     * @param output   Base Output stream, usually a facade around Java stream.
     * @param openMode Open mode that was specified
     */
    public LogicalStream(PrologInteger id, PrologInputStream input, PrologOutputStream output, StreamProperties.OpenMode openMode) {
        this.id = id;
        this.baseInput = input;
        this.baseOutput = output;
        this.openMode = openMode;
        this.ioChanged = true; // set each time IO needs reconfiguring
    }

    private LogicalStream() {
        this.id = PrologInteger.from(-1);
        this.baseInput = null;
        this.baseOutput = null;
        this.openMode = null;
        this.ioChanged = false;
        this.closed = true;
    }

    /**
     * Close streams.
     *
     * @throws IOException IO Error
     */
    public synchronized boolean close(Environment environment, CloseOptions options) throws IOException {
        if (closed) {
            return false; // cannot close streams that have previously been closed
        }
        if (isProtected(environment) && !options.force) {
            return false;
        }
        if (!((input == null || input.approveClose(options))
                && (output == null || output.approveClose(options)))) {
            return false;
        }
        if (input != null) {
            input.close(options);
        }
        if (output != null) {
            output.close(options);
        }
        closed = true;
        return true;
    }

    /**
     * Flush streams.
     *
     * @throws IOException IO Error
     */
    public void flush() throws IOException {
        // only output needs flush
        if (output != null) {
            output.flush();
        }
    }

    /**
     * @return Id of stream
     */
    public PrologInteger getId() {
        return this.id;
    }

    /**
     * Add alias to list/set of aliases. List is assumed to be small.
     *
     * @param alias Alias to add
     */
    public void removeAlias(PrologAtomLike alias) {
        // We assume few (typically 0 or 1) aliases, so this is maintained as a list
        // rather than as a set.
        aliases.remove(alias);
    }

    /**
     * Add alias to list/set of aliases. Aliases has already been dedup'd before call.
     *
     * @param alias Alias to add
     */
    public void addAlias(PrologAtomInterned alias) {
        // We assume few (typically 0 or 1) aliases, so this is maintained as a list
        // rather than as a set. We also assume alias is not already in list (enfored
        // by caller).
        aliases.add(alias);
    }

    /**
     * @return Retrieve first stream alias if any, else return stream id
     */
    public Atomic getAlias() {
        if (aliases.isEmpty()) {
            return id;
        } else {
            return aliases.get(0);
        }
    }

    /**
     * @return Retrieve all aliases.
     */
    public ArrayList<PrologAtomInterned> getAliases() {
        ArrayList<PrologAtomInterned> atoms = new ArrayList<>();
        atoms.addAll(aliases);
        return atoms;
    }

    /**
     * @param bufferMode New buffer mode
     */
    public void setBufferMode(StreamProperties.Buffering bufferMode) {
        this.bufferMode = bufferMode;
        this.ioChanged = true;
    }

    /**
     * @return Current buffer mode
     */
    public StreamProperties.Buffering getBufferMode() {
        return bufferMode;
    }

    /**
     * @param size New buffer size
     */
    public void setBufferSize(long size) {
        if (bufferMode != StreamProperties.Buffering.ATOM_false) {
            bufferSize = size;
            this.ioChanged = true;
        }
    }

    /**
     * @return buffer size
     */
    public Long getBufferSize() {
        if (bufferSize != null) {
            return bufferSize;
        } else {
            throw new FutureFlagKeyError(new PrologAtom("buffer_size"));
        }
    }

    /**
     * @return True if BOM was detected
     */
    public boolean isBomDetected() {
        return false; // TODO
    }

    /**
     * @param closeOnAbort New close on abort mode
     */
    public void setCloseOnAbort(Boolean closeOnAbort) {
        this.closeOnAbort = closeOnAbort;
    }

    /**
     * @return Current close on abort mode
     */
    public Boolean getCloseOnAbort() {
        return closeOnAbort;
    }

    /**
     * @param closeOnExec New close on exec mode
     */
    public void setCloseOnExec(Boolean closeOnExec) {
        this.closeOnExec = closeOnExec;
    }

    /**
     * @return Current close on exec mode
     */
    public Boolean getCloseOnExec() {
        return closeOnExec;
    }

    /**
     * @param encoding Change encoding
     */
    public void setEncoding(StreamProperties.Encoding encoding) {
        this.encoding = encoding;
        this.ioChanged = true;
    }

    /**
     * @return Current encoding
     */
    public StreamProperties.Encoding getEncoding() {
        return encoding;
    }

    /**
     * @param eofAction Change EOF action
     */
    public void setEofAction(StreamProperties.EofAction eofAction) {
        this.eofAction = eofAction;
    }

    /**
     * @return Current EOF action
     */
    public StreamProperties.EofAction getEofAction() {
        return eofAction;
    }

    /**
     * @param fileName Change descriptive filename
     */
    public void setObjectTerm(Term fileName) {
        this.fileName = fileName;
    }

    /**
     * @return Get descriptive filename
     */
    public Term getFileName() {
        return fileName;
    }

    /**
     * @return Get open mode (used by current_stream)
     */
    public PrologAtomInterned getMode() {
        if (isOutput()) {
            return Io.OPEN_WRITE;
        } else {
            return Io.OPEN_READ;
        }
    }

    /**
     * @param newLineMode Change new line mode
     */
    public void setNewLineMode(StreamProperties.NewLineMode newLineMode) {
        this.newLineMode = newLineMode;
    }

    /**
     * @return Current new line mode
     */
    public StreamProperties.NewLineMode getNewLineMode() {
        return newLineMode;
    }

    /**
     * @param type Change encoding type
     */
    public void setType(StreamProperties.Type type) {
        this.type = type;
    }

    /**
     * @return Current encoding type
     */
    public StreamProperties.Type getType() {
        return type;
    }

    /**
     * @param recordPosition Start/reset recording of positions
     */
    public void setRecordPosition(Boolean recordPosition) {
        this.recordPosition = recordPosition;
    }

    /**
     * @return true if position information is being recorded
     */
    public Boolean getRecordPosition() {
        return recordPosition;
    }

    /**
     * @param isTTY Indicate TTY mode
     */
    public void setIsTTY(Boolean isTTY) {
        this.isTTY = isTTY;
        this.ioChanged = true;
    }

    /**
     * @return True if stream is in TTY mode
     */
    public Boolean getIsTTY() {
        return isTTY;
    }

    /**
     * @return True if has input substream
     */
    public boolean isInput() {
        return input != null;
    }

    /**
     * @return True if has output substream
     */
    public boolean isOutput() {
        return output != null;
    }

    /**
     * @return Original open mode
     */
    public StreamProperties.OpenMode getOpenMode() {
        return openMode;
    }

    /**
     * @return Term providing line/char/byte positions
     */
    public Term getPosition() {
        PrologStream stream = getStream();
        Position pos = new Position();
        try {
            stream.getPosition(pos);
            ArrayList<Term> terms = new ArrayList<>();
            mapOptionalPosElement(terms, Io.BYTE_POS, pos.getBytePos());
            mapOptionalPosElement(terms, Io.CHAR_POS, pos.getCharPos());
            mapOptionalPosElement(terms, Io.COLUMN_POS, pos.getColumnPos());
            mapOptionalPosElement(terms, Io.LINE_POS, pos.getLinePos());
            return TermList.from(terms);
        } catch (IOException ioe) {
            // TODO: correct error?
            return PrologEmptyList.EMPTY_LIST;
        }
    }

    public void restorePosition(Environment environment, Atomic streamIdent, Term positionTerm) {
        PrologStream stream = getStream();
        Position pos = new Position();
        if (CompoundTerm.termIsA(positionTerm, Io.END_OF_STREAM, 1) &&
                ((CompoundTerm) positionTerm).get(0).compareTo(Io.AT) == 0) {
            // seek to end of stream
            seekEndOfStream(environment, streamIdent, stream);
            return;
        }
        TermList.TermIterator iter = TermList.listIterator(positionTerm);
        while (iter.hasNext()) {
            Term element = iter.next();
            if (!(element instanceof CompoundTerm)) {
                throw PrologTypeError.compoundExpected(environment, element);
            }
            CompoundTerm posEl = (CompoundTerm) element;
            if (posEl.functor() == Io.BYTE_POS && posEl.arity() == 1) {
                pos.setBytePos(PrologInteger.from(posEl.get(0)).notLessThanZero().toLong());
            } else if (posEl.functor() == Io.CHAR_POS && posEl.arity() == 1) {
                pos.setCharPos(PrologInteger.from(posEl.get(0)).notLessThanZero().toLong());
            } else if (posEl.functor() == Io.LINE_POS && posEl.arity() == 1) {
                pos.setLinePos(PrologInteger.from(posEl.get(0)).notLessThanZero().toLong());
            } else if (posEl.functor() == Io.COLUMN_POS && posEl.arity() == 1) {
                pos.setColumnPos(PrologInteger.from(posEl.get(0)).notLessThanZero().toLong());
            } else {
                throw PrologDomainError.error(environment, "stream_position", positionTerm);
            }
        }
        try {
            if (stream.seekPosition(pos)) {
                return;
            }
        } catch (IOException ioe) {
            // ignore, error below
        }
        throw PrologPermissionError.error(environment, "modify", "position", streamIdent,
                String.format("Cannot modify stream position on %s", streamIdent));
    }

    private void seekEndOfStream(Environment environment, Atomic name, PrologStream stream) {
        try {
            stream.seekEndOfStream();
        } catch (IOException e) {
            throw PrologPermissionError.error(environment, "reposition", "stream", name, "Cannot reposition on stream");
        }
    }

    private static void mapOptionalPosElement(ArrayList<Term> list, Atomic name, Optional<Long> element) {
        if (element.isPresent()) {
            list.add(new CompoundTermImpl(name, PrologInteger.from(element.get())));
        }
    }

    /**
     * @param linePosition Set new effective line position
     */
    public void setLinePosition(long linePosition) {
        PrologStream stream = getStream();
        Position pos = new Position();
        pos.setLinePos(linePosition);
        try {
            stream.seekPosition(pos);
        } catch (IOException ioe) {
            // ignored
        }
    }

    /**
     * @return Current line position
     */
    public Long getLinePosition() {
        PrologStream stream = getStream();
        Position pos = new Position();
        try {
            stream.getPosition(pos);
        } catch (IOException ioe) {
            // ignored
        }
        return pos.getLinePos().orElse(0L);
    }

    /**
     * @return True if seekable
     */
    public boolean canReposition() {
        return seekable;
    }

    /**
     * Change sequence of filters
     */
    private void configure() {
        ioChanged = false;
        if (recordPosition && tracker == null) {
            tracker = new PositionTracker();
        }
        if (baseInput != null) {
            input = configureInput(baseInput);
        }
        if (baseOutput != null) {
            output = configureOutput(baseOutput);
        }
    }

    private Charset getCharset() {
        if (type == StreamProperties.Type.ATOM_text) {
            switch (encoding) {
                case ATOM_ascii:
                    return StandardCharsets.US_ASCII;
                case ATOM_iso_latin_1:
                    return StandardCharsets.ISO_8859_1;
                case ATOM_utf8:
                    return StandardCharsets.UTF_8;
                case ATOM_unicode_be:
                    return StandardCharsets.UTF_16BE;
                case ATOM_unicode_le:
                    return StandardCharsets.UTF_16LE;
            }
        }
        return null; // no charset encoding
    }

    private void assertText(Environment environment, PrologAtomLike action, Term streamIdent) {
        if (type != StreamProperties.Type.ATOM_text) {
            throw PrologPermissionError.error(environment, action, Io.BINARY_STREAM, streamIdent,
                    String.format("Error %s to binary stream %s", action, streamIdent));
        }
    }

    private void assertBinary(Environment environment, PrologAtomLike action, Term streamIdent) {
        if (type != StreamProperties.Type.ATOM_binary) {
            throw PrologPermissionError.error(environment, action, Io.TEXT_STREAM, streamIdent,
                    String.format("Error %s to text stream %s", action, streamIdent));
        }
    }

    private PrologOutputStream configureOutput(PrologOutputStream source) {
        Position savedPos = null;
        if (source != null) {
            try {
                Position pos = new Position();
                source.getPosition(pos);
                savedPos = pos;
            } catch (IOException io) {
                // ignored
            }
        }
        PrologOutputStream filtered = source;
        Charset cs = getCharset();
        if (cs != null) {
            filtered = new OutputEncoderFilter(filtered, cs);
        }
        // TODO: layers to handle TTY
        // TODO: layers to handle buffering
        if (recordPosition) {
            filtered = new OutputPositionTracker(filtered, tracker);
        }
        if (savedPos != null) {
            filtered.setKnownPosition(savedPos);
        }
        return filtered;
    }

    private PrologInputStream configureInput(PrologInputStream source) {
        Position savedPos = null;
        if (source != null) {
            try {
                Position pos = new Position();
                source.getPosition(pos);
                savedPos = pos;
            } catch (IOException io) {
                // ignored
            }
        }
        PrologInputStream filtered = source;
        // layers to handle TTY
        if (isTTY) {
            // TODO: how should this find the right output stream?
            filtered = new InputPrompter(filtered, new SequentialOutputStream(System.err));
        }
        if (type == StreamProperties.Type.ATOM_text) {
            Charset cs = getCharset();
            if (cs != null) {
                filtered = new InputDecoderFilter(filtered, cs);
            }
            // text is always line handled and buffered
            filtered = new InputBuffered(new InputLineHandler(filtered, newLineMode), -1);
            if (recordPosition) {
                // can only track position if it is not an IO stream
                filtered = new InputPositionTracker(filtered, tracker);
            }
        } else {
            // layer(s) to handle buffering
            switch (bufferMode) {
                case ATOM_line:
                    filtered = new InputBuffered(new InputLineHandler(filtered, newLineMode), -1);
                    break;
                case ATOM_full:
                    filtered = new InputBuffered(filtered, -1);
                    break;
            }
        }
        if (savedPos != null) {
            filtered.setKnownPosition(savedPos);
        }
        return filtered;
    }

    /**
     * Retrieve associated input substream.
     *
     * @param environment Execution environment
     * @param streamId    Id for error reporting (or null if unknown)
     * @return input substream.
     */
    public PrologInputStream getInputStream(Environment environment, Atomic streamId) {
        if (ioChanged) {
            configure();
        }
        if (input != null) {
            return input;
        } else {
            // cannot input
            Atomic id = Optional.ofNullable(streamId).orElse(this.id);
            throw PrologPermissionError.error(environment, "input", "stream", id,
                    String.format("Stream '%s' not open for input", id.toString()));
        }
    }

    /*
     * Retrieve associated output substream.
     *
     * @param environment Execution environment
     * @param streamId    Id for error reporting (or null if unknown)
     * @return output substream.
     */
    public PrologOutputStream getOutputStream(Environment environment, Atomic streamId) {
        if (ioChanged) {
            configure();
        }
        if (output != null) {
            return output;
        } else {
            // cannot output
            Atomic id = Optional.ofNullable(streamId).orElse(this.id);
            throw PrologPermissionError.error(environment, "output", "stream", id,
                    String.format("Stream '%s' not open for output", id.toString()));
        }
    }

    /**
     * /**
     * Retrieve a reference to something seekable
     *
     * @return substream.
     */
    public PrologStream getStream() {
        if (ioChanged) {
            configure();
        }
        if (output != null) {
            return output;
        } else {
            assert input != null;
            return input;
        }
    }

    /**
     * Get a single character from stream
     *
     * @param environment Execution environment
     * @param streamId    stream id for error reporting (or null if unknown)
     */
    public Atomic getChar(Environment environment, Atomic streamId) {
        assertText(environment, Io.INPUT_ACTION, streamId);
        PrologInputStream input = getInputStream(environment, streamId);
        try {
            int c = input.read();
            if (c < 0) {
                return Io.END_OF_FILE;
            }
            return new PrologCharacter((char) c);
        } catch (IOException ioe) {
            throw PrologError.systemError(environment, ioe);
        }
    }

    /**
     * Get a single character code from stream
     *
     * @param environment Execution environment
     * @param streamId    stream id for error reporting (or null if unknown)
     */
    public Atomic getCode(Environment environment, Atomic streamId) {
        assertText(environment, Io.INPUT_ACTION, streamId);
        PrologInputStream input = getInputStream(environment, streamId);
        try {
            int c = input.read();
            if (c < 0) {
                return Io.END_OF_FILE;
            }
            return PrologInteger.from(c);
        } catch (IOException ioe) {
            throw PrologError.systemError(environment, ioe);
        }
    }

    /**
     * Get a single byte from stream
     *
     * @param environment Execution environment
     * @param streamId    stream id for error reporting (or null if unknown)
     */
    public Atomic getByte(Environment environment, Atomic streamId) {
        assertBinary(environment, Io.INPUT_ACTION, streamId);
        PrologInputStream input = getInputStream(environment, streamId);
        try {
            int c = input.read();
            if (c < 0) {
                return Io.END_OF_FILE;
            }
            return PrologInteger.from(c);
        } catch (IOException ioe) {
            throw PrologError.systemError(environment, ioe);
        }
    }

    /**
     * Specify/change active prompt
     *
     * @param environment Execution environment
     * @param streamId    stream id for error reporting (or null if unknown)
     * @param prompt      Prompt to specify
     */
    public void setPrompt(Environment environment, Atomic streamId, Prompt prompt) {
        PrologInputStream input = getInputStream(environment, streamId);
        input.setPrompt(prompt);
    }

    /**
     * Internal write text
     *
     * @param environment Execution environment
     * @param streamId    stream id for error reporting (or null if unknown)
     * @param text        String to write
     */
    public void write(Environment environment, Atomic streamId, String text) {
        PrologOutputStream output = getOutputStream(environment, streamId);
        try {
            output.write(text);
        } catch (IOException ioe) {
            throw PrologError.systemError(environment, ioe);
        }
    }

    /**
     * Internal write symbol
     *
     * @param environment Execution environment
     * @param streamId    stream id for error reporting (or null if unknown)
     * @param symbol      Symbol to write
     */
    public void write(Environment environment, Atomic streamId, int symbol) {
        PrologOutputStream output = getOutputStream(environment, streamId);
        try {
            output.write(symbol);
        } catch (IOException ioe) {
            throw PrologError.systemError(environment, ioe);
        }
    }

    /**
     * Put single character
     *
     * @param environment Execution environment
     * @param streamId    Stream id for error reporting (or null if unknown)
     * @param target      Character to write
     */
    public void putChar(Environment environment, Atomic streamId, Term target) {
        assertText(environment, Io.OUTPUT_ACTION, streamId);
        if (!target.isInstantiated()) {
            throw PrologInstantiationError.error(environment, target);
        }
        String text;
        if (target.isInteger()) {
            int symbol = PrologInteger.from(target).toChar();
            write(environment, streamId, symbol);
        } else if (target.isAtom()) {
            text = PrologAtomLike.from(target).name();
            if (text.length() != 1) {
                throw PrologTypeError.characterExpected(environment, target);
            }
            write(environment, streamId, text);
        } else {
            throw PrologTypeError.characterExpected(environment, target);
        }
    }

    /**
     * Put single byte
     *
     * @param environment Execution environment
     * @param streamId    Stream id for error reporting (or null if unknown)
     * @param target      Byte to write
     */
    public void putByte(Environment environment, Atomic streamId, Term target) {
        assertBinary(environment, Io.OUTPUT_ACTION, streamId);
        if (!target.isInstantiated()) {
            throw PrologInstantiationError.error(environment, target);
        }
        int val = PrologInteger.from(target).toInteger();
        PrologOutputStream output = getOutputStream(environment, streamId);
        try {
            output.write(val);
        } catch (IOException ioe) {
            throw PrologError.systemError(environment, ioe);
        }
    }

    /**
     * Read a line of text
     *
     * @param environment Execution environment
     * @param streamId    Stream id for error reporting (or null if unknown)
     * @param options     Read options
     * @return read line
     */
    public String readLine(Environment environment, Atomic streamId, ReadOptions options) {
        assertText(environment, Io.INPUT_ACTION, streamId);
        PrologInputStream input = getInputStream(environment, streamId);
        if (options == null) {
            options = new ReadOptions(environment, null);
        }
        try {
            return input.readLine();
        } catch (IOException ioe) {
            throw PrologError.systemError(environment, ioe);
        }
    }

    /**
     * Read a term from an expression
     *
     * @param environment Execution environment
     * @param streamId    Stream id for error reporting (or null if unknown)
     * @param options     Read options
     * @return read token
     */
    public Term read(Environment environment, Atomic streamId, ReadOptions options) {
        assertText(environment, Io.INPUT_ACTION, streamId);
        PrologInputStream input = getInputStream(environment, streamId);
        if (options == null) {
            options = new ReadOptions(environment, null);
        }
        Tokenizer tokenizer = new Tokenizer(environment, options, input);
        ExpressionReader reader = new ExpressionReader(tokenizer);
        return reader.read();
    }

    /**
     * Read a term from an expression
     *
     * @param environment Execution environment
     * @param streamId    Stream id for error reporting (or null if unknown)
     * @param target      Target unified with term
     * @param optionsTerm Term providing options
     */
    public void read(Environment environment, Atomic streamId, Term target, Term optionsTerm) {
        assertText(environment, Io.INPUT_ACTION, streamId);
        Term value = read(environment, streamId, new ReadOptions(environment, optionsTerm));
        if (!Unifier.unify(environment.getLocalContext(), target, value)) {
            environment.backtrack();
        }
    }

    /**
     * Write a term as an expression
     *
     * @param environment Execution environment
     * @param streamId    Stream id for error reporting (or null if unknown)
     * @param source      Source term to write
     * @param options     Write options
     */
    public void write(Environment environment, Atomic streamId, Term source, WriteOptions options) {
        assertText(environment, Io.OUTPUT_ACTION, streamId);
        PrologOutputStream output = getOutputStream(environment, streamId);
        if (!source.isInstantiated()) {
            throw PrologInstantiationError.error(environment, source);
        }
        WriteContext context = new WriteContext(environment, options, output);
        StructureWriter writer = new StructureWriter(context);

        try {
            writer.write(source);
            if (options.fullstop) {
                writer.write(Interned.DOT);
                if (options.nl) {
                    output.write("\n");
                } else {
                    output.write(" ");
                }
            } else if (options.nl) {
                output.write("\n");
            }
        } catch (IOException ioe) {
            throw writeError(ioe, environment);
        }
    }

    /**
     * Write an end of line
     *
     * @param environment Execution environment
     * @param streamId    Stream identifier (or null if unknown)
     */
    public void nl(Environment environment, Atomic streamId) {
        assertText(environment, Io.OUTPUT_ACTION, streamId);
        PrologOutputStream output = getOutputStream(environment, streamId);
        try {
            // TODO: Need to consider text NL type
            output.write("\n");
        } catch (IOException ioe) {
            throw writeError(ioe, environment);
        }
    }

    /**
     * Translates a read error
     *
     * @param cause       IO exception
     * @param environment Execution environment
     * @return error to throw
     */
    private static PrologError readError(Throwable cause, Environment environment) {
        return PrologError.systemError(environment, cause);
    }

    /**
     * Translates a write error
     *
     * @param cause       IO exception
     * @param environment Execution environment
     * @return error to throw
     */
    private static PrologError writeError(Throwable cause, Environment environment) {
        return PrologError.systemError(environment, cause);
    }

    /**
     * A stream is a default for the given environment.
     *
     * @param environment Environment that has default reference
     * @param mask        mask of what is considered protected
     */
    public synchronized void protect(Environment environment, int mask) {
        if (protection == null) {
            protection = new WeakHashMap<>();
        }
        protection.compute(environment, (key, prior) -> prior == null ? mask : mask | prior);
    }

    /**
     * Remove default-ness protection
     *
     * @param environment Environment that has default reference
     * @param mask        indicating which protection to remove
     */
    public synchronized void unprotect(Environment environment, int mask) {
        if (protection == null) {
            return;
        }
        protection.computeIfPresent(environment, (key, prior) -> (prior & ~mask) == 0 ? null : prior & ~mask);
        if (protection.isEmpty()) {
            protection = null; // remove map if no longer applying protection
        }
    }

    /**
     * True if stream is considered protected
     *
     * @param environment Environment to check for
     * @return true if protected
     */
    public synchronized boolean isProtected(Environment environment) {
        return protection != null && protection.containsKey(environment);
    }

    /**
     * True if stream is considered already closed
     *
     * @return true if closed
     */
    public synchronized boolean isClosed() {
        return closed;
    }
}
