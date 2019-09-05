package prolog.suite;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

public class VanillaTest extends Suite {


    @Disabled("Vanilla suite ignored, it will fail")
    @Test
    public void testVanillaSuite() {
        runSuite(Paths.get("vanilla"),
                "?- ['vanilla.pl'].",
                "?- validate.");
    }
}
