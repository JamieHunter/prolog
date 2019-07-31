package prolog.io;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.flags.ReadOptions;
import prolog.flags.StreamProperties;
import prolog.test.Given;
import prolog.test.PrologTest;
import prolog.test.StreamUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
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
        try(LogicalStream stream = StreamUtils.logicalFileStream(logFile.toPath(), StandardOpenOption.READ)) {
            term = stream.read(environment, null, new ReadOptions(environment, null));
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
                "a(2) :- false.",
                "a(3) :- true."
                );
        given().when("?- consult('"+path+"').")
                .assertSuccess()
                .andWhen("?- a(1).")
                .assertSuccess()
                .andWhen("?- a(2).")
                .assertFailed()
                .andWhen("?- a(3).") // a(3) in same file as a(1), compare with testConsultList
                .assertSuccess();
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
                "x(1).",
                "a(1).",
                ":- expectLog('a')."
        );
        String path2 = createFile(testFolder.newFile(),
                "x(2).",
                "b(2).",
                ":- expectLog('b')."
        );
        String path3 = createFile(testFolder.newFile(),
                "x(3).",
                "c(3).",
                ":- expectLog('c')."
        );
        given().when("?- ['"+path1+"','"+path2+"','"+path3+"'].")
                .assertSuccess()
                .andWhen("?- expectLog('.').")
                .assertSuccess()
                .andWhen("?- a(1), b(2), c(3).")
                .assertSuccess()
                .andWhen("?- x(1).")
                .assertFailed() // x/1 overwritten
                .andWhen("?- x(2).")
                .assertFailed() // x/1 overwritten
                .andWhen("?- x(3).")
                .assertSuccess() // x/1 final version
                ;

        checkLog(isAtom("abc"));
    }

    @Test
    public void testEnsureLoaded() throws IOException {
        String path1 = createFile(testFolder.newFile(),
                "a(1).",
                "a(2)."
        );
        String path2 = createFile(testFolder.newFile(),
                "a(3)."
        );
        // also test the ".pl" extension renaming
        String pathOut = testFolder.newFile().toString() + ".pl";
        Files.copy(Paths.get(path1), Paths.get(pathOut)); // first version
        given().when("?- ensure_loaded('"+path1+"').")
                .assertSuccess()
                .andWhen("?- a(1), a(2).")
                .assertSuccess()
                .andWhen(w -> {
                    try {
                        Files.copy(Paths.get(path2), Paths.get(pathOut), StandardCopyOption.REPLACE_EXISTING);
                    } catch(IOException ioe) {
                        fail(ioe.getMessage());
                    }
                })
                .andWhen("?- a(1), a(2).")
                .assertSuccess() // did not override these
                .andWhen("?- a(3).")
                .assertFailed() // did not load new file
        ;
    }
}
