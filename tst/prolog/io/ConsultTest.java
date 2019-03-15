package prolog.io;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.flags.ReadOptions;
import prolog.test.Given;
import prolog.test.PrologTest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertThat;
import static prolog.test.Matchers.*;

public class ConsultTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private File logFile;
    private String logPath;

    @Before
    public void setLog() throws IOException {
        logFile = testFolder.newFile();
        logPath = quotePath(logFile);
    }

    private String quotePath(File path) {
        return path.getPath().replace("\\", "\\\\").replace("'", "''");
    }

    private String createFile(File file, String ... lines) throws IOException {
        try(BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            for(String line : lines) {
                writer.write(line);
                writer.write("\n");
            }
        }
        return quotePath(file);
    }

    private void checkLog(Matcher<? super Term> m) throws IOException {
        Term term;
        Environment environment = new Environment();
        try(PrologReadStream reader = new PrologReadStreamImpl(logFile.toPath())) {
            term = reader.read(environment, new ReadOptions(environment, null));
        }
        assertThat(term, m);
    }

    private Given given() {
        return PrologTest.given(
                "expectLog(X) :- open('"+logPath+"', append, File), "+
                        "current_output(Last), " +
                        "set_output(File), " +
                        "write(X), " +
                        "set_output(Last), " +
                        "close(File).");
    }

    @Test
    public void testLogging() throws IOException {
        // this verifies the logging above works
        given().when("?- expectLog('a'), expectLog('b'), expectLog('.').").assertSuccess();
        checkLog(isAtom("ab"));
    }

    @Test
    public void testSingleFileConsult() throws IOException {
        String path = createFile(testFolder.newFile(),
                "a(1).",
                "a(2) :- false."
                );
        given().when("?- consult('"+path+"').")
                .assertSuccess()
                .andWhen("?- a(1).")
                .assertSuccess()
                .andWhen("?- a(2).")
                .assertFailed();
    }

    @Test
    public void testConsultWithDirectives() throws IOException {
        String path = createFile(testFolder.newFile(),
                "a(1).",
                "a(2).",
                ":- a(X), expectLog(X), expectLog('.')."
        );
        given().when("?- consult('"+path+"').")
                .assertSuccess();
        checkLog(isInteger(1));
    }

    @Test
    public void testConsultList() throws IOException {
        String path1 = createFile(testFolder.newFile(),
                "a(1).",
                "?- expectLog('a')."
        );
        String path2 = createFile(testFolder.newFile(),
                "a(2).",
                "?- expectLog('b')."
        );
        String path3 = createFile(testFolder.newFile(),
                "a(3).",
                "?- expectLog('c')."
        );
        given().when("?- ['"+path1+"','"+path2+"','"+path3+"'].")
                .assertSuccess()
                .andWhen("?- a(1), a(2), a(3).")
                .assertSuccess()
                .andWhen("?- expectLog('.').")
                .assertSuccess();
        checkLog(isAtom("abc"));
    }
}
