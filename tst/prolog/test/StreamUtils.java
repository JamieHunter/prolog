package prolog.test;

import prolog.flags.StreamProperties;
import prolog.io.InputBuffered;
import prolog.io.InputDecoderFilter;
import prolog.io.LogicalStream;
import prolog.io.PrologInputStream;
import prolog.io.PrologOutputStream;
import prolog.io.RandomAccessStream;
import prolog.io.SequentialInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
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

    public static RandomAccessStream fileStream(Path path, OpenOption... options) throws IOException {
        return new RandomAccessStream(FileChannel.open(path, options));
    }

    public static LogicalStream logicalFileStream(Path path, OpenOption... options) throws IOException {
        RandomAccessStream stream = fileStream(path, options);
        return new LogicalStream(LogicalStream.unique(), stream, stream, StreamProperties.OpenMode.ATOM_update);
    }
}
