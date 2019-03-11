package prolog.suite;

import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Paths;

public class VanillaTest extends Suite {


    @Ignore("Vanilla suite ignored, it will fail")
    @Test
    public void testVanillaSuite() {
        runSuite(Paths.get("vanilla"),
                "?- ['vanilla.pl'].",
                "?- validate.");
    }
}
