package org.jprolog.io;

import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

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
