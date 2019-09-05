package prolog.io;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import prolog.test.PrologTest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;
import static prolog.test.Matchers.*;

public class ReadWriteTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private String quotePath(File path) {
        return path.getPath().replace("\\", "\\\\").replace("'", "''");
    }

    @Test
    public void testOpenAndWrite() throws IOException {
        File tempFile = testFolder.newFile("openAndWrite.txt");
        String path = quotePath(tempFile);
        PrologTest.given()
                .when("?- open('"+path+"', write, File), "+
                        "set_output(File), "+
                        "write(testing), "+
                        "close(File).")
                .assertSuccess();
        try(BufferedReader reader = Files.newBufferedReader(tempFile.toPath(), StandardCharsets.UTF_8)) {
            String text = reader.readLine();
            assertThat(text, equalTo("testing"));
        }
    }

    @Test
    public void testOpenAndRead() throws IOException {
        File tempFile = testFolder.newFile("openAndRead.txt");
        String path = quotePath(tempFile);
        try(BufferedWriter writer = Files.newBufferedWriter(tempFile.toPath(), StandardCharsets.UTF_8)) {
            writer.write("a(b,c).");
        }
        PrologTest.given()
                .when("?- open('"+path+"', read, File), "+
                        "set_input(File), "+
                        "read(X), "+
                        "close(File).")
                .assertSuccess()
                .variable("X", isCompoundTerm("a", isAtom("b"), isAtom("c")));
    }

    @Test
    public void testOpenAndAppend() throws IOException {
        File tempFile = testFolder.newFile("openAndAppend.txt");
        String path = quotePath(tempFile);
        try(BufferedWriter writer = Files.newBufferedWriter(tempFile.toPath(), StandardCharsets.UTF_8)) {
            writer.write("foo");
        }
        PrologTest.given()
                .when("?- open('"+path+"', append, File), "+
                        "set_output(File), "+
                        "write(bar), "+
                        "close(File).")
                .assertSuccess();
        try(BufferedReader reader = Files.newBufferedReader(tempFile.toPath(), StandardCharsets.UTF_8)) {
            String text = reader.readLine();
            assertThat(text, equalTo("foobar"));
        }
    }
}
