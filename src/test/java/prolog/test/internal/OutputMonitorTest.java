package prolog.test.internal;

import org.junit.jupiter.api.Test;
import prolog.exceptions.PrologHalt;
import prolog.test.Given;
import prolog.test.PrologTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static prolog.test.Matchers.isInteger;

/**
 * Sanity test to make sure OutputMonitor behaves as expected
 */
public class OutputMonitorTest {

    @Test
    public void testOutputMonitorGrep() {
        PrologTest.given()
                .when("?- '##text_log'(log, \"^\\\\[Failed\\\\]\").")
                .assertSuccess()
                .andWhen("?- write(log, '[Failed] Pass'), nl(log).")
                .assertSuccess()
                .textMatches("log", is(1));
    }

    @Test
    public void testOutputMonitorGrepWithSilence() {
        PrologTest.given()
                .when("?- '##text_log'(log, \"-^\\\\[Failed\\\\]\").")
                .assertSuccess()
                .andWhen("?- write(log, '[Failed] Pass'), nl(log).")
                .assertSuccess()
                .textMatches("log", is(1));
    }

    @Test
    public void testOutputMonitorInProlog() {
        PrologTest.given()
                .when("?- '##text_log'(log, \"-\\\\[Failed\\\\]\").")
                .assertSuccess()
                .andWhen("?- write(log, 'Foo [Failed] Pass').")
                .assertSuccess()
                .andWhen("?- '##text_matches'(log, X).")
                .assertSuccess()
                .variable("X", isInteger(1));
    }
}
