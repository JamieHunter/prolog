package org.jprolog.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;

public class OutputEncoderFilterTest {
    //
    // Streamed output, converting to byte stream in the process
    //
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private final OutputEncoderFilter stream = new OutputEncoderFilter(
            new SequentialOutputStream(buffer),
            StandardCharsets.UTF_8);

    @Test
    public void testWrite1() throws IOException {
        stream.write('H');
        stream.write('\u0903');
        assertBuffer("H\u0903");
    }

    @Test
    public void testWriteString() throws IOException {
        stream.write("Hello\n\u0903\u0904\uD83c\uDf09\nEnd");
        assertBuffer("Hello\n\u0903\u0904\uD83c\uDf09\nEnd");
    }

    @Test
    public void testWriteSymbols() throws IOException {
        stream.write("\u0903\u0904\uD83c\uDf09xx".toCharArray(), 1, 3);
        assertBuffer("\u0904\uD83c\uDf09");
        stream.write("xx\u0903yy".toCharArray(), 1, 3);
        assertBuffer("\u0904\uD83c\uDf09x\u0903y");
    }

    private static Integer[] conv(byte... data) {
        Integer[] conv = new Integer[data.length];
        for (int i = 0; i < data.length; i++) {
            conv[i] = (int) data[i];
        }
        return conv;
    }

    private static void assertArrays(Integer[] a, Integer[] b) {
        assertThat(a, arrayContaining(b));
    }

    private static void assertArrays(byte[] a, byte[] b) {
        assertArrays(conv(a), conv(b));
    }

    private void assertBuffer(String test) {
        assertArrays(buffer.toByteArray(), test.getBytes());
    }

    private void assertPosition(long expected) throws IOException {
        Position pos = new Position();
        stream.getPosition(pos);
        assertThat(pos.getCharPos().isPresent(), is(true));
        assertThat(pos.getCharPos().get(), is(expected));
    }
}
