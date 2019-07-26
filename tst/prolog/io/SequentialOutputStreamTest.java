package prolog.io;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class SequentialOutputStreamTest {

    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private SequentialOutputStream stream = new SequentialOutputStream(buffer);

    private String asString() {
        return new String(buffer.toByteArray());
    }

    @Test
    public void testWrite1() throws IOException {
        stream.write('H');
        stream.write('x');
        assertThat(asString(), is(equalTo("Hx")));
    }

    @Test
    public void testWriteString() throws IOException {
        stream.write("World");
        assertThat(asString(), is(equalTo("World")));
    }

    @Test
    public void testWriteBytes() throws IOException {
        stream.write("Bytes".getBytes(), 2, 2);
        assertThat(asString(), is(equalTo("te")));
        stream.write("More".getBytes(), 0, 3);
        assertThat(asString(), is(equalTo("teMor")));
        stream.write("Done!".getBytes(), 0, 5);
        assertThat(asString(), is(equalTo("teMorDone!")));
    }

    @Test
    public void testWriteSymbols() throws IOException {
        stream.write("Bytes".toCharArray(), 2, 2);
        assertThat(asString(), is(equalTo("te")));
        stream.write("More".toCharArray(), 0, 3);
        assertThat(asString(), is(equalTo("teMor")));
        stream.write("Done!".toCharArray(), 0, 5);
        assertThat(asString(), is(equalTo("teMorDone!")));
    }

    @Test
    public void testFlush() {
        // TODO, to be implemented
        //stream.flush();
    }

    @Test
    public void testClose() {
        // TODO, to be implemented
        //stream.close();
    }

    @Test
    public void testGetPosition() throws IOException {
        // Note, this is tested further in other classes
        Position pos = new Position();
        stream.getPosition(pos);
        assertThat(pos.getBytePos().isPresent(), is(true));
        assertThat(pos.getBytePos().get(), is(0L));
        stream.write("Data");
        stream.getPosition(pos);
        assertThat(pos.getBytePos().get(), is(4L));
    }

    @Test
    public void testRestorePosition() throws IOException {
        // Note, this is tested further in other classes
        Position pos = new Position();
        stream.getPosition(pos);
        stream.write("Data");
        boolean changed = stream.restorePosition(pos);
        assertThat(changed, is(false));
    }
}
