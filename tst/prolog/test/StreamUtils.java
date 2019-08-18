package prolog.test;

import prolog.constants.PrologInteger;
import prolog.execution.Environment;
import prolog.flags.CloseOptions;
import prolog.flags.StreamProperties;
import prolog.io.InputBuffered;
import prolog.io.InputDecoderFilter;
import prolog.io.LogicalStream;
import prolog.io.PrologInputStream;
import prolog.io.PrologOutputStream;
import prolog.io.FileReadWriteStreams;
import prolog.io.SequentialInputStream;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Path;

public final class StreamUtils {
    private StreamUtils() {
    }

    public static InputStream javaBytes(byte[] data) {
        return new ByteArrayInputStream(data);
    }

    public static InputStream javaString(String data) {
        return javaBytes(data.getBytes());
    }

    public static PrologInputStream prologBytes(byte[] data) {
        return new InputBuffered(new SequentialInputStream(javaBytes(data)), -1);
    }

    public static PrologInputStream prologString(String data) {
        return new SequentialInputStream(javaString(data));
    }

    public static PrologInputStream bufferedString(String data) {
        return new InputBuffered(new InputDecoderFilter(prologString(data), StandardCharsets.UTF_8), -1);
    }

    public static LogicalStream logical(PrologInputStream stream) {
        return new LogicalStream(LogicalStream.unique(), stream, null, StreamProperties.OpenMode.ATOM_read);
    }

    public static LogicalStream logical(PrologOutputStream stream) {
        return new LogicalStream(LogicalStream.unique(), null, stream, StreamProperties.OpenMode.ATOM_write);
    }

    public static FileReadWriteStreams fileStream(Path path, OpenOption... options) throws IOException {
        return new FileReadWriteStreams(FileChannel.open(path, options));
    }

    public static TestLogicalStream logicalFileStream(Environment environment, Path path, OpenOption... options) throws IOException {
        FileReadWriteStreams stream = fileStream(path, options);
        return new TestLogicalStream(environment, LogicalStream.unique(), stream, stream, StreamProperties.OpenMode.ATOM_update);
    }

    public static class TestLogicalStream extends LogicalStream implements Closeable {

        private final Environment environment;

        public TestLogicalStream(Environment environment, PrologInteger id, PrologInputStream input, PrologOutputStream output, StreamProperties.OpenMode openMode) {
            super(id, input, output, openMode);
            this.environment = environment;
        }

        @Override
        public void close() throws IOException {
            close(new CloseOptions(environment, null));
        }
    }
}
