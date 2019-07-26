// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import prolog.constants.Atomic;
import prolog.constants.PrologAtom;
import prolog.constants.PrologCharacter;
import prolog.constants.PrologInteger;
import prolog.exceptions.PrologError;
import prolog.exceptions.PrologInstantiationError;
import prolog.exceptions.PrologPermissionError;
import prolog.exceptions.PrologTypeError;
import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.flags.ReadOptions;
import prolog.flags.StreamProperties;
import prolog.flags.WriteOptions;
import prolog.library.Io;
import prolog.parser.ExpressionReader;
import prolog.parser.Tokenizer;
import prolog.unification.Unifier;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Associates a constant with an input and output substream pair. Generally only one is used, but this allows
 * for a read/write logical stream.
 */
public class LogicalStream implements Closeable {

    // provides a unique identifier over lifetime of execution.
    private static final AtomicInteger counter = new AtomicInteger(1);

    /**
     * Create a unique stream ID
     *
     * @return new stream ID
     */
    public static PrologInteger unique() {
        int n = counter.getAndIncrement();
        return new PrologInteger(BigInteger.valueOf(n));
    }

    private final PrologInteger id;
    private final PrologInputStream baseInput;
    private final PrologOutputStream baseOutput;
    private PrologInputStream input = null;
    private PrologOutputStream output = null;

    private List<PrologAtom> aliases = new ArrayList<>();
    private StreamProperties.OpenMode openMode;
    private StreamProperties.NewLineMode newLineMode = StreamProperties.NewLineMode.ATOM_detect;
    private StreamProperties.Buffering bufferMode = StreamProperties.Buffering.ATOM_full;
    private Integer bufferSize = null;
    private boolean closeOnAbort = true;
    private boolean closeOnExec = true;
    private StreamProperties.Encoding encoding = StreamProperties.Encoding.ATOM_utf8;
    private StreamProperties.EofAction eofAction = StreamProperties.EofAction.ATOM_error;
    private StreamProperties.Type type = StreamProperties.Type.ATOM_text;
    private Term fileName;
    private int linePosition = 0;
    private long charPosition = 0;
    private long bytePosition = 0;
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

    /**
     * Close streams.
     *
     * @throws IOException IO Error
     */
    public void close() throws IOException {
        if (input != null) {
            input.close();
        }
        if (output != null) {
            output.close();
        }
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
    public void removeAlias(PrologAtom alias) {
        // We assume few (typically 0 or 1) aliases, so this is maintained as a list
        // rather than as a set.
        aliases.remove(alias);
    }

    /**
     * Add alias to list/set of aliases. Aliases has already been dedup'd before call.
     *
     * @param alias Alias to add
     */
    public void addAlias(PrologAtom alias) {
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
    public void setBufferSize(int size) {
        if (bufferMode != StreamProperties.Buffering.ATOM_false) {
            bufferSize = size;
            this.ioChanged = true;
        }
    }

    /**
     * @return buffer size
     */
    public Integer getBufferSize() {
        if (bufferSize != null) {
            return bufferSize;
        } else {
            throw new UnsupportedOperationException("NYI");
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
    public void setFileName(Term fileName) {
        this.fileName = fileName;
    }

    /**
     * @return Get descriptive filename
     */
    public Term getFileName() {
        return fileName;
    }

    /**
     * @param linePosition Set new line position
     */
    public void setLinePosition(int linePosition) {
        this.linePosition = linePosition;
    }

    /**
     * @return Current line position
     */
    public Integer getLinePosition() {
        return linePosition;
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
        throw new UnsupportedOperationException("NYI");
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
    private void configure(Environment environment) {
        ioChanged = false;
        if (baseInput != null) {
            input = configureInput(environment, baseInput);
        }
        if (baseOutput != null) {
            output = configureOutput(environment, baseOutput);
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

    private PrologOutputStream configureOutput(Environment environment, PrologOutputStream source) {
        PrologOutputStream filtered = source;
        Charset cs = getCharset();
        if (cs != null) {
            filtered = new OutputEncoderFilter(filtered, cs);
        }
        // TODO: layers to handle TTY
        // TODO: layers to handle buffering
        return filtered;
    }

    private PrologInputStream configureInput(Environment environment, PrologInputStream source) {
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
            configure(environment);
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

    /**
     * Retrieve associated output substream.
     *
     * @param environment Execution environment
     * @param streamId    Id for error reporting (or null if unknown)
     * @return output substream.
     */
    public PrologOutputStream getOutputStream(Environment environment, Atomic streamId) {
        if (ioChanged) {
            configure(environment);
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
     * Get a single character from stream
     *
     * @param environment Execution environment
     * @param streamId    stream id for error reporting (or null if unknown)
     */
    public Atomic getChar(Environment environment, Atomic streamId) {
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
     * Put single character
     *
     * @param environment Execution environment
     * @param streamId    Stream id for error reporting (or null if unknown)
     * @param target      Character to write
     */
    public void putChar(Environment environment, Atomic streamId, Term target) {
        if (!target.isInstantiated()) {
            throw PrologInstantiationError.error(environment, target);
        }
        String text;
        if (target.isInteger()) {
            text = String.valueOf((char) PrologInteger.from(target).get().intValue());
        } else if (target.isAtom()) {
            text = PrologAtom.from(target).get();
            if (text.length() != 1) {
                throw PrologTypeError.characterExpected(environment, target);
            }
        } else {
            throw PrologTypeError.characterExpected(environment, target);
        }
        write(environment, streamId, text);
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
        Term value = read(environment, streamId, new ReadOptions(environment, optionsTerm));
        Unifier.unify(environment.getLocalContext(), target, value);
    }

    /**
     * Write a term as an expression
     *
     * @param environment Execution environment
     * @param streamId    Stream id for error reporting (or null if unknown)
     * @param source      Source term to write
     * @param optionsTerm Term providing options
     */
    public void write(Environment environment, Atomic streamId, Term source, Term optionsTerm) {
        PrologOutputStream output = getOutputStream(environment, streamId);
        if (!source.isInstantiated()) {
            throw PrologInstantiationError.error(environment, source);
        }
        WriteOptions options = new WriteOptions(environment, optionsTerm);
        WriteContext context = new WriteContext(environment, options, output);
        StructureWriter writer = new StructureWriter(context, source);

        try {
            writer.write();
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
}
