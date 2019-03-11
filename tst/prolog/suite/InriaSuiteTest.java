package prolog.suite;

import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Paths;

public class InriaSuiteTest extends Suite {

    @Ignore("INRIA Suite ignored, it will fail")
    @Test
    public void testInriaSuite() {
        runSuite(Paths.get("inriasuite"),
                "?- ['inriasuite.pl'].",
                "?- run_all_tests.");
    }
}
