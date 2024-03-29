package org.jprolog.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * A file can be opened in a read/write mode of operation, the underlying helper class is
 * FileReadWriteStreams
 */
public class FileReadWriteStreamsTest {

    @TempDir
    public File testFolder;

    private File tempFile;
    private FileReadWriteStreams stream;
    private FileChannel observer;

    @BeforeEach
    public void prepareTest() throws IOException {
        tempFile = new File(testFolder, "random.bin");
        stream = new FileReadWriteStreams(FileChannel.open(tempFile.toPath(), StandardOpenOption.CREATE,
                StandardOpenOption.READ, StandardOpenOption.WRITE));
    }

    @AfterEach
    public void endTest() throws IOException {
        stream.close();
    }

    @Test
    public void testWriteShort() throws IOException {
        byte[] testDataShort = new byte[123];
        ThreadLocalRandom.current().nextBytes(testDataShort);
        // Simple write-then-observe
        stream.write(testDataShort, 0, testDataShort.length);
        stream.flush();
        assertThat(tempFile.length(), is((long) testDataShort.length));
        assertPosition(testDataShort.length);
        assertData(0, testDataShort);
    }

    @Test
    public void testWriteMedium() throws IOException {
        byte[] testDataMedium = new byte[253];
        ThreadLocalRandom.current().nextBytes(testDataMedium);
        // Simple write-then-observe
        stream.write(testDataMedium, 0, testDataMedium.length);
        stream.flush();
        assertThat(tempFile.length(), is((long) testDataMedium.length));
        assertPosition(testDataMedium.length);
        assertData(0, testDataMedium);
    }

    @Test
    public void testWriteLong() throws IOException {
        byte[] testDataLong = new byte[23253];
        ThreadLocalRandom.current().nextBytes(testDataLong);
        // Simple write-then-observe
        stream.write(testDataLong, 0, testDataLong.length);
        stream.flush();
        assertThat(tempFile.length(), is((long) testDataLong.length));
        assertPosition(testDataLong.length);
        assertData(0, testDataLong);
    }

    @Test
    public void testWriteSeekWrite() throws IOException {
        byte[] testOuter = new byte[23253];
        ThreadLocalRandom.current().nextBytes(testOuter);
        byte[] testInner = new byte[513];
        ThreadLocalRandom.current().nextBytes(testInner);
        stream.write(testOuter, 0, testOuter.length);
        final int base = testOuter.length - 1000;
        setPosition(base);
        stream.write(testInner, 0, testInner.length);
        stream.flush();
        assertThat(tempFile.length(), is((long) testOuter.length));
        assertPosition(base + testInner.length);
        assertData(base, testInner); // test this subset first
        System.arraycopy(testInner, 0, testOuter, base, testInner.length);
        assertData(0, testOuter); // now make sure entire content is as expected
    }

    @Test
    public void testWriteSeekReadSeekRead() throws IOException {
        byte[] testOuter = new byte[991];
        ThreadLocalRandom.current().nextBytes(testOuter);
        stream.write(testOuter, 0, testOuter.length);
        assertPosition(testOuter.length);
        int base = testOuter.length - 100;
        setPosition(base);
        byte[] readData = new byte[90];
        stream.read(readData, 0, readData.length);
        byte[] expected = new byte[readData.length];
        System.arraycopy(testOuter, base, expected, 0, expected.length);
        assertArrays(readData, expected);
        base = base + readData.length - 300;
        setPosition(base);
        stream.read(readData, 0, readData.length);
        System.arraycopy(testOuter, base, expected, 0, expected.length);
        assertArrays(readData, expected);
    }

    @Test
    public void testWriteReadWriteRead() throws IOException {
        byte[] testOuter = new byte[991];
        ThreadLocalRandom.current().nextBytes(testOuter);
        stream.write(testOuter, 0, testOuter.length);
        assertPosition(testOuter.length);
        int base = testOuter.length - 100;
        setPosition(base);
        byte[] readData = new byte[20];
        stream.read(readData, 0, readData.length);
        assertPosition(base + readData.length);
        byte[] expected = new byte[readData.length];
        System.arraycopy(testOuter, base, expected, 0, expected.length);
        assertArrays(readData, expected);
        base = base + 10;
        setPosition(base);
        byte[] layerWrite = new byte[25];
        ThreadLocalRandom.current().nextBytes(layerWrite);
        stream.write(layerWrite, 0, layerWrite.length);
        assertPosition(base + layerWrite.length);
        System.arraycopy(layerWrite, 0, testOuter, base, layerWrite.length);
        base = base - 20;
        setPosition(base);
        byte[] layerRead = new byte[50];
        stream.read(layerRead, 0, layerRead.length);
        assertPosition(base + layerRead.length);
        expected = new byte[layerRead.length];
        System.arraycopy(testOuter, base, expected, 0, expected.length);
        assertArrays(layerRead, expected);
    }

    @Test
    public void testWriteThenReadBytes() throws IOException {
        byte[] testOuter = new byte[991];
        ThreadLocalRandom.current().nextBytes(testOuter);
        for (int i = 0; i < testOuter.length; i++) {
            stream.write(testOuter[i]);
        }
        assertPosition(testOuter.length);
        int base = testOuter.length - 100;
        setPosition(base);
        byte[] readData = new byte[90];
        for (int i = 0; i < readData.length; i++) {
            int c = stream.read();
            readData[i] = (byte) c;
            assertThat(c, greaterThanOrEqualTo(0));
        }
        byte[] expected = new byte[readData.length];
        System.arraycopy(testOuter, base, expected, 0, expected.length);
        assertArrays(readData, expected);
    }

    private void setPosition(long position) throws IOException {
        Position pos = new Position();
        pos.setBytePos(position);
        boolean success = stream.seekPosition(pos);
        assertThat(success, is(true));
    }

    private void assertPosition(long expected) throws IOException {
        Position pos = new Position();
        stream.getPosition(pos);
        assertThat(pos.getBytePos().isPresent(), is(true));
        assertThat(pos.getBytePos().get(), is(expected));
    }

    //
    // Utility, read back from file to observe what changes were made
    //
    private byte[] observe(int start, int len) throws IOException {
        try (FileChannel channel = FileChannel.open(tempFile.toPath(), StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate(len);
            int total = 0;
            while (total < len) {
                int chunk = channel.read(buffer, start + total);
                if (chunk <= 0) {
                    break;
                }
                total += chunk;
            }
            buffer.flip();
            byte[] result = new byte[total];
            buffer.get(result);
            return result;
        }
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

    private void assertData(int start, byte[] expected) throws IOException {
        byte[] observed = observe(start, expected.length);
        assertArrays(observed, expected);
    }
}
