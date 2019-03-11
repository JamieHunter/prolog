// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import prolog.exceptions.PrologError;
import prolog.execution.Environment;
import prolog.expressions.Term;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

/**
 * Implementation class for Prolog write streams.
 */
public class PrologWriteStreamImpl extends PrologStream implements PrologWriteStream {
    private final Writer writer;

    /**
     * Create a stream from a name and a writer.
     *
     * @param sinkName Name of sink
     * @param writer   Java writer
     */
    public PrologWriteStreamImpl(String sinkName, Writer writer) {
        super(sinkName);
        this.writer = writer;
    }

    /**
     * Open a stream for writing to a file.
     *
     * @param path    Path to file
     * @param options Set of write options applied when file is opened
     * @throws IOException on IO error
     */
    public PrologWriteStreamImpl(Path path, OpenOption... options) throws IOException {
        super(path.toString());
        writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        writer.close();
    }

    /**
     * {@inheritDoc}
     */
    public void write(String s) throws IOException {
        writer.write(s);
    }

    /**
     * {@inheritDoc}
     */
    public void write(Environment environment, Term term) {
        try {
            WriteContext context = new WriteContext(environment, this);
            StructureWriter structWriter = new StructureWriter(context, term);
            structWriter.write();
        } catch (IOException ioe) {
            throw PrologError.systemError(environment, ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IOException {
        writer.flush();
    }

    /**
     * {@inheritDoc}
     */
    public Writer javaWriter() {
        return writer;
    }
}
