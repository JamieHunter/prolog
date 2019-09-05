package prolog.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

public class InputBufferedTest {

    //
    // Input buffering has ability to rewind and move around. This is incorporated into these tests.
    //

    private static byte[] testData = "Hello\nWorld\nOne\nTwo\nThree\nFour\nFive".getBytes();
    private static final int BUFFER_SIZE = 7; // intentionally prime, small, extreme

    private InputBuffered stream = new InputBuffered(
            new SequentialInputStream(new ByteArrayInputStream(testData)),
            BUFFER_SIZE);

    private int unsplitRead(char[] buffer, int off, int len) throws IOException {
        // with the small buffer, many reads will be split.
        int sublen = stream.read(buffer, off, len);
        if (sublen == 0) {
            fail("first read returned zero characters");
        }
        if (sublen < len && sublen > 0) {
            int sublen2 = stream.read(buffer, off + sublen, len - sublen);
            if (sublen2 == 0) {
                fail("second read returned zero characters");
            }
            if (sublen2 > 0) {
                sublen += sublen2;
            }
        }
        return sublen;
    }

    @Test
    public void testSimpleRead1() throws IOException {
        // Regression test vs base implementation
        for (int i = 0; i < testData.length; i++) {
            int c = stream.read();
            assertThat(c, is((int) testData[i]));
        }
        assertThat(stream.read(), is(-1));
        assertThat(stream.read(), is(-1));
    }

    @Test
    public void testBacktrackRead1() throws IOException {
        // Using simple read function, read, backtrack, read
        Position pos = new Position();
        assertPosition(0L);
        stream.advance(6);
        assertPosition(6L);
        stream.getPosition(pos);
        // Advance and confirm read at this point
        for (int i = 6; i < 12; i++) {
            int c = stream.read();
            assertThat(c, is((int) testData[i]));
        }
        // Rewind
        stream.seekPosition(pos);
        // Expect to re-read what was just read
        for (int i = 6; i < 12; i++) {
            int c = stream.read();
            assertThat(c, is((int) testData[i]));
        }
        stream.getPosition(pos);
        // Expect to be able to advance forward (this will cross buffer boundary)
        for (int i = 12; i < 19; i++) {
            int c = stream.read();
            assertThat(c, is((int) testData[i]));
        }
        // Rewind again
        stream.seekPosition(pos);
        // And verify content again to end
        for (int i = 12; i < testData.length; i++) {
            int c = stream.read();
            assertThat(c, is((int) testData[i]));
        }
        assertPosition(testData.length);
        assertThat(stream.read(), is(-1));
        assertThat(stream.read(), is(-1));
    }

    @Test
    public void testBacktrackReadSymbols() throws IOException {
        Position pos = new Position();
        stream.advance(6);
        assertPosition(6);
        stream.getPosition(pos);
        char[] target1 = new char[10];
        int len = unsplitRead(target1, 0, 5); // into start of array
        assertThat(len, is(5));
        assertPosition(11);
        assertArrays(target1, 'W', 'o', 'r', 'l', 'd', '\0', '\0', '\0', '\0', '\0');
        len = unsplitRead(target1, 3, 2); // into mid array
        assertThat(len, is(2));
        assertPosition(13);
        assertArrays(target1, 'W', 'o', 'r', '\n', 'O', '\0', '\0', '\0', '\0', '\0');
        // Rewind
        stream.seekPosition(pos);
        assertPosition(6);
        // overwrite
        len = unsplitRead(target1, 3, 6);
        assertThat(len, is(6));
        assertPosition(12);
        assertArrays(target1, 'W', 'o', 'r', 'W', 'o', 'r', 'l', 'd', '\n', '\0');
        stream.advance(4);
        assertPosition(16);
        len = unsplitRead(target1, 6, 4); // single byte into mid array
        assertThat(len, is(4));
        assertPosition(20);
        assertArrays(target1, 'W', 'o', 'r', 'W', 'o', 'r', 'T', 'w', 'o', '\n');
        stream.getPosition(pos);
        pos.setCharPos(pos.getCharPos().get() + 6);
        stream.seekPosition(pos);
        assertPosition(26);
        len = unsplitRead(target1, 1, 5);
        assertThat(len, is(5));
        assertPosition(31);
        assertArrays(target1, 'W', 'F', 'o', 'u', 'r', '\n', 'T', 'w', 'o', '\n');
        stream.getPosition(pos);
        pos.setCharPos(pos.getCharPos().get() - 6);
        stream.seekPosition(pos);
        assertPosition(25);
        len = unsplitRead(target1, 0, 1);
        assertThat(len, is(1));
        assertPosition(26);
        assertArrays(target1, '\n', 'F', 'o', 'u', 'r', '\n', 'T', 'w', 'o', '\n');
        stream.advance(5);
        assertPosition(31);
        len = unsplitRead(target1, 2, 8);
        assertThat(len, is(4));
        assertPosition(35);
        assertArrays(target1, '\n', 'F', 'F', 'i', 'v', 'e', 'T', 'w', 'o', '\n');
        len = unsplitRead(target1, 1, 9); // read past end
        assertThat(len, is(-1));
        assertPosition(35);
        assertArrays(target1, '\n', 'F', 'F', 'i', 'v', 'e', 'T', 'w', 'o', '\n');
    }

    @Test
    public void testReadLine() throws IOException {
        // Also available
        assertThat(stream.available(), is(testData.length));
        String text = stream.readLine(); // read up to EOLN
        assertThat(text, is(equalTo("Hello")));
        assertThat(stream.available(), is(testData.length - 6));
        text = stream.readLine(); // read up to EOLN
        assertThat(text, is(equalTo("World")));
        text = stream.readLine(); // read up to EOLN
        assertThat(text, is(equalTo("One")));
        text = stream.readLine(); // read up to EOLN
        assertThat(text, is(equalTo("Two")));
        text = stream.readLine(); // read up to EOLN
        assertThat(text, is(equalTo("Three")));
        text = stream.readLine(); // read up to EOLN
        assertThat(text, is(equalTo("Four")));
        assertThat(stream.available(), is(4));
        text = stream.readLine(); // read up to EOLN
        assertThat(text, is(equalTo("Five")));
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
        assertThat(pos.getBytePos().isPresent(), is(false));
        assertThat(pos.getCharPos().isPresent(), is(true));
        assertThat(pos.getCharPos().get(), is(0L));
        String text = stream.readLine(); // read up to EOLN
        stream.getPosition(pos);
        assertThat(pos.getCharPos().get(), is(6L));
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
