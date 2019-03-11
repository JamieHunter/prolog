// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import prolog.constants.Atomic;
import prolog.constants.PrologCharacter;
import prolog.exceptions.PrologError;
import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.library.Io;
import prolog.parser.ExpressionReader;
import prolog.parser.Tokenizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Implementation class for Prolog read streams.
 */
public class PrologReadStreamImpl extends PrologStream implements PrologReadStream {

    private final BufferedReader reader;

    /**
     * Create a stream from a name and a reader.
     *
     * @param sourceName Name of source
     * @param reader         Java buffered reader
     */
    public PrologReadStreamImpl(String sourceName, BufferedReader reader) {
        super(sourceName);
        this.reader = reader;
    }

    /**
     * Open a stream for reading from a file.
     *
     * @param path Path to file
     * @throws IOException on IO error
     */
    public PrologReadStreamImpl(Path path) throws IOException {
        super(path.toString());
        reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
    }

    /**
     * {@inheritDoc}
     */
    public BufferedReader javaReader() {
        return reader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        reader.close();
    }

    /**
     * {@inheritDoc}
     */
    public Atomic getChar(Environment environment) {
        try {
            int c = reader.read();
            if (c < 0) {
                return Io.END_OF_FILE;
            }
            return new PrologCharacter((char) c);
        } catch (IOException ioe) {
            throw PrologError.systemError(environment, ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Term read(Environment environment) {
        Tokenizer tokenizer = new Tokenizer(environment, this);
        ExpressionReader reader = new ExpressionReader(tokenizer);
        return reader.read();
    }

    /**
     * {@inheritDoc}
     */
    public void setPrompt(Prompt prompt) {
        // does nothing by default
    }
}
