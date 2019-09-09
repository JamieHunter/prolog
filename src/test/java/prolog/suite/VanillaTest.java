package prolog.suite;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.hamcrest.Matchers.is;

public class VanillaTest extends Suite {

    @Test
    public void testVanillaSuite() {
        given(Paths.get("vanilla"))
                .when("?- '##text_log'(log, \"^\\\\[Failed\\\\]\"), set_output(log), '##set_default_streams'.")
                .andWhen("?- ['vanilla.pl'].").assertSuccess()
                .andWhen("?- validate.").assertSuccess()
                .textMatches("log", is(0));
    }
}
