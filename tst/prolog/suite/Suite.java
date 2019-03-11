package prolog.suite;

import prolog.test.internal.StateImpl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class Suite {

    protected static final String USER_DIR = "user.dir";
    protected void runSuite(Path directory, String ... goals) {
        Path cwd = Paths.get("suites").toAbsolutePath();
        Path testDir = cwd.resolve(directory).normalize().toAbsolutePath();
        assertTrue("Directory not found", Files.isDirectory(testDir));
        StateImpl state = new StateImpl();
        state.environment().setCWD(testDir);
        for(String goal: goals) {
            state.parse(goal);
            assertTrue("failed: " + goal, state.succeeded());
        }
    }
}
