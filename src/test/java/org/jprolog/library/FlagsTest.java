package org.jprolog.library;

import org.junit.jupiter.api.Test;
import org.jprolog.exceptions.PrologPermissionError;
import org.jprolog.test.PrologTest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.jprolog.test.Matchers.isAtom;
import static org.jprolog.test.Matchers.isInteger;

public class FlagsTest {

    @Test
    public void testWellKnownFlags() {
        PrologTest.given().when("?- current_prolog_flag(bounded, X).")
                .assertSuccess()
                .variable("X", isAtom("false"));
        PrologTest.given().when("?- current_prolog_flag(back_quotes, X).")
                .assertSuccess()
                .variable("X", isAtom("codes"));
        PrologTest.given().when("?- current_prolog_flag(double_quotes, X).")
                .assertSuccess()
                .variable("X", isAtom("string"));
        PrologTest.given().when("?- current_prolog_flag(break_level, X).")
                .assertSuccess()
                .variable("X", isInteger(0));
    }

    @Test
    public void testUpdateProtectedFlag() {
        assertThrows(PrologPermissionError.class, ()->{
            PrologTest.given().when("?- set_prolog_flag(bounded, true).");
        });
    }

    @Test
    public void testCreateUseCustomFlag() {
        PrologTest.given("?- create_prolog_flag(thingy, 15, [type(integer), access(read_write)]).")
                .when("?- current_prolog_flag(thingy, X).")
                .assertSuccess()
                .variable("X", isInteger(15))
                .andWhen("?- set_prolog_flag(thingy, 16).")
                .assertSuccess()
                .andWhen("?- current_prolog_flag(thingy, X).")
                .assertSuccess()
                .variable("X", isInteger(16));
    }

    @Test
    public void testCreateConstCustomFlag() {
        assertThrows(PrologPermissionError.class, ()->{
            PrologTest.given("?- create_prolog_flag(thingy, foo, [access(read_only)]).")
                    .when("?- set_prolog_flag(thingy, bar).");
        });
    }
}
