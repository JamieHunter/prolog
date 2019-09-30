package org.jprolog.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.jupiter.api.Assertions.*;

public class InputDecoderFilterTest {

    //
    // Streamed input, converting to specified character set in the process.
    //

    private static final String testString = "Hello\n\u0903\u0904\uD83c\uDf09\nEnd";
    private static final char[] chars = testString.toCharArray();
    private static final byte[] testData = testString.getBytes();

    private final InputDecoderFilter stream = new InputDecoderFilter(
            new SequentialInputStream(new ByteArrayInputStream(testData)),
            StandardCharsets.UTF_8);

    @Test
    public void testSimpleRead1() throws IOException {
        // Regression test vs base implementation
        assertTrue(testData.length != chars.length, "Assume arrays mismatch");
        for (int i = 0; i < chars.length; i++) {
            int c = stream.read();
            assertThat(c, is((int) chars[i]));
        }
        assertThat(stream.read(), is(-1));
        assertThat(stream.read(), is(-1));
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
        assertArrays(target1, 'H', 'e', 'l', '\n', '\u0903', '\u0904', '\uD83c', '\uDf09', '\n', '\0');
        len = stream.read(target1, 1, 9); // read until end
        assertThat(len, is(3));
        assertPosition(14);
        assertArrays(target1, 'H', 'E', 'n', 'd', '\u0903', '\u0904', '\uD83c', '\uDf09', '\n', '\0');
        len = stream.read(target1, 1, 9); // read past end
        assertThat(len, is(-1));
        assertPosition(14);
        assertArrays(target1, 'H', 'E', 'n', 'd', '\u0903', '\u0904', '\uD83c', '\uDf09', '\n', '\0');
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
