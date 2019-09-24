package org.jprolog.suite;

import org.jprolog.test.Given;
import org.jprolog.test.PrologTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Suite {

    protected static final String USER_DIR = "user.dir";

    protected Given given(Path directory) {
        Path cwd = Paths.get("src/test/prolog").toAbsolutePath();
        Path testDir = cwd.resolve(directory).normalize().toAbsolutePath();
        assertTrue(Files.isDirectory(testDir), "Directory not found");
        return PrologTest.given().environment(e -> e.setCWD(testDir));
    }
}
