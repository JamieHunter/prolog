package org.jprolog.io;

import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class OpenStringTest {

    @Test
    public void testOpenAndRead() throws IOException {
        PrologTest.given()
                .when("?- open_string(`a(b,c).`, Stream), " +
                        "read(Stream, X), " +
                        "close(Stream).")
                .assertSuccess()
                .variable("X", Matchers.isCompoundTerm("a", Matchers.isAtom("b"), Matchers.isAtom("c")));
    }
}
