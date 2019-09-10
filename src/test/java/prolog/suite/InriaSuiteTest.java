package prolog.suite;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

public class InriaSuiteTest extends Suite {

    @Disabled("INRIA Suite ignored, it will fail")
    @Test
    public void testInriaSuite() {
        given(Paths.get("inriasuite"))
                .when("?- ['inriasuite.pl'].").assertSuccess()
                .andWhen("?- run_all_tests.").assertSuccess();
    }

    @Disabled("Exists to help debug INRIA Suite")
    @Test
    public void testInriaSuiteMicro() {
        given(Paths.get("inriasuite"))
                .when("?- ['inriasuite.pl'].").assertSuccess()
                .andWhen("?- run_tests(zzz).").assertSuccess();
    }
}
