package prolog.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class SequentialInputStreamTest {

    private static byte[] testData = "Hello\nWorld\n123".getBytes();

    private SequentialInputStream stream = new SequentialInputStream(new ByteArrayInputStream(testData));

    @Test
    public void testRead1() throws IOException {
        for (int i = 0; i < testData.length; i++) {
            int c = stream.read();
            assertThat(c, is((int) testData[i]));
        }
        assertThat(stream.read(), is(-1));
        assertThat(stream.read(), is(-1));
    }

    @Test
    public void testReadBytes() throws IOException {
        // Note, this is tested further in other classes
        byte[] target1 = new byte[10];
        int len = stream.read(target1, 0, 5); // into start of array
        assertThat(len, is(5));
        assertArrays(target1, 'H', 'e', 'l', 'l', 'o', '\0', '\0', '\0', '\0', '\0');
        len = stream.read(target1, 3, 6); // into mid array
        assertThat(len, is(6));
        assertArrays(target1, 'H', 'e', 'l', '\n', 'W', 'o', 'r', 'l', 'd', '\0');
        len = stream.read(target1, 7, 1); // single byte into mid array
        assertThat(len, is(1));
        assertArrays(target1, 'H', 'e', 'l', '\n', 'W', 'o', 'r', '\n', 'd', '\0');
        len = stream.read(target1, 1, 9); // read until end
        assertThat(len, is(3));
        assertArrays(target1, 'H', '1', '2', '3', 'W', 'o', 'r', '\n', 'd', '\0');
        len = stream.read(target1, 1, 9); // read past end
        assertArrays(target1, 'H', '1', '2', '3', 'W', 'o', 'r', '\n', 'd', '\0');
    }

    @Test
    public void testReadSymbols() throws IOException {
        // Note, this is tested further in other classes
        char[] target1 = new char[10];
        int len = stream.read(target1, 0, 5); // into start of array
        assertThat(len, is(5));
        assertPosition(5);
        assertArrays(target1, 'H', 'e', 'l', 'l', 'o', '\0', '\0', '\0', '\0', '\0');
        len = stream.read(target1, 3, 6); // into mid array
        assertThat(len, is(6));
        assertPosition(11);
        assertArrays(target1, 'H', 'e', 'l', '\n', 'W', 'o', 'r', 'l', 'd', '\0');
        len = stream.read(target1, 7, 1); // single byte into mid array
        assertThat(len, is(1));
        assertPosition(12);
        assertArrays(target1, 'H', 'e', 'l', '\n', 'W', 'o', 'r', '\n', 'd', '\0');
        len = stream.read(target1, 1, 9); // read until end
        assertThat(len, is(3));
        assertPosition(15);
        assertArrays(target1, 'H', '1', '2', '3', 'W', 'o', 'r', '\n', 'd', '\0');
        len = stream.read(target1, 1, 9); // read past end
        assertThat(len, is(-1));
        assertPosition(15);
        assertArrays(target1, 'H', '1', '2', '3', 'W', 'o', 'r', '\n', 'd', '\0');
    }

    @Test
    public void testReadLineAndAvailable() throws IOException {
        // Also available
        assertThat(stream.available(), is(testData.length));
        String text = stream.readLine(); // read up to EOLN
        assertThat(text, is(equalTo("Hello")));
        assertThat(stream.available(), is(testData.length - 6));
        text = stream.readLine(); // read up to EOLN
        assertThat(text, is(equalTo("World")));
        assertThat(stream.available(), is(3));
        text = stream.readLine(); // read up to end of file
        assertThat(text, is(equalTo("123")));
        assertThat(stream.available(), is(0));
        text = stream.readLine(); // at end
        assertThat(text, is(nullValue()));
        assertThat(stream.available(), is(0));
        text = stream.readLine(); // past end
        assertThat(text, is(nullValue()));
        assertThat(stream.available(), is(0));
    }

    @Test
    public void testClose() {
        // TODO
    }

    @Test
    public void testGetPosition() throws IOException {
        // Note, this is tested further in other classes
        Position pos = new Position();
        stream.getPosition(pos);
        assertThat(pos.getBytePos().isPresent(), is(true));
        assertThat(pos.getBytePos().get(), is(0L));
        String text = stream.readLine(); // read up to EOLN
        stream.getPosition(pos);
        assertThat(pos.getBytePos().get(), is(6L));
    }

    @Test
    public void testRestorePosition() throws IOException {
        // Note, this is tested further in other classes
        Position pos = new Position();
        pos.setBytePos(1);
        boolean changed = stream.seekPosition(pos);
        assertThat(changed, is(false));
        pos = new Position();
        pos.setCharPos(1);
        changed = stream.seekPosition(pos);
        assertThat(changed, is(false));
    }


    private static Integer[] conv(byte... data) {
        Integer[] conv = new Integer[data.length];
        for (int i = 0; i < data.length; i++) {
            conv[i] = (int) data[i];
        }
        return conv;
    }

    private static Integer[] conv(char... data) {
        Integer[] conv = new Integer[data.length];
        for (int i = 0; i < data.length; i++) {
            conv[i] = (int) data[i];
        }
        return conv;
    }

    private static void assertArrays(Integer[] a, Integer[] b) {
        assertThat(a, arrayContaining(b));
    }

    private static void assertArrays(byte[] a, char... b) {
        assertArrays(conv(a), conv(b));
    }

    private static void assertArrays(char[] a, char... b) {
        assertArrays(conv(a), conv(b));
    }

    private void assertPosition(long expected) throws IOException {
        Position pos = new Position();
        stream.getPosition(pos);
        assertThat(pos.getCharPos().isPresent(), is(true));
        assertThat(pos.getCharPos().get(), is(expected));
    }

}
