package prolog.suite;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

public class InriaSuiteTest extends Suite {

    @Disabled("INRIA Suite ignored, it will fail")
    @Test
    public void testInriaSuite() {
        runSuite(Paths.get("inriasuite"),
                "?- ['inriasuite.pl'].",
                "?- run_all_tests.");
    }
}
