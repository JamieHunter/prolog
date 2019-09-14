package org.jprolog.library;

import org.jprolog.exceptions.PrologHalt;
import org.jprolog.test.Given;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SystemControlTest {

    private Given given() {
        return PrologTest.given();
    }

    @Test
    public void testHalt() {
        PrologHalt halt = assertThrows(PrologHalt.class, () -> {
            given().when("?- halt.");
        });
        assertThat(halt.getHaltCode(), is(0));
    }

    @Test
    public void testHaltWithNumber() {
        PrologHalt halt = assertThrows(PrologHalt.class, () -> {
            given().when("?- halt(10).");
        });
        assertThat(halt.getHaltCode(), is(10));
    }
}
