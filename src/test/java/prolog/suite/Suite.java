package prolog.suite;

import prolog.test.Given;
import prolog.test.PrologTest;
import prolog.test.Then;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Suite {

    protected static final String USER_DIR = "user.dir";

    protected Given given(Path directory) {
        Path cwd = Paths.get("src/test/prolog").toAbsolutePath();
        Path testDir = cwd.resolve(directory).normalize().toAbsolutePath();
        assertTrue("Directory not found", Files.isDirectory(testDir));
        return PrologTest.given().cwd(testDir);
    }
}
