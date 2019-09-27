package org.jprolog.library;

import org.jprolog.exceptions.PrologSyntaxError;
import org.junit.jupiter.api.Test;
import org.jprolog.test.Given;
import org.jprolog.test.PrologTest;

import static org.jprolog.test.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests various conversions
 */
public class ConversionsTest {

    protected Given given() {
        return PrologTest.given();
    }

    @Test
    public void testAtomChars() {
        given()
                .when("?- atom_chars(foo, [f,o,o]).")
                .assertSuccess()
                .andWhen("?- atom_chars(X, [b,a,r]).")
                .assertSuccess()
                .variable("X", isAtom("bar"))
                .andWhen("?- atom_chars(bx, Y).")
                .assertSuccess()
                .variable("Y", isList(isAtom("b"), isAtom("x")))
                .andWhen("?- atom_chars(Q, `12`).")
                .assertSuccess()
                .variable("Q", isAtom("12"))
                .andWhen("?- atom_string(Q, \"pq\").")
                .assertSuccess()
                .variable("Q", isAtom("pq"));
    }

    @Test
    public void testAtomCodes() {
        given()
                .when("?- atom_codes(X, [65, 66]).")
                .assertSuccess()
                .variable("X", isAtom("AB"))
                .andWhen("?- atom_codes(ab, Y).")
                .assertSuccess()
                .variable("Y", isList(isInteger('a'), isInteger('b')))
        ;
    }

    @Test
    public void testCharCode() {
        given()
                .when("?- char_code('1', 0'1).")
                .assertSuccess()
                .andWhen("?- char_code('1', 0'2).")
                .assertFailed()
                .andWhen("?- char_code(a, X).")
                .assertSuccess()
                .variable("X", isInteger('a'))
                .andWhen("?- char_code(X, 65).")
                .assertSuccess()
                .variable("X", isAtom("A"));
    }

    @Test
    public void testNumberChars() {
        given()
                .when("?- number_chars(X, ['3', '.', '3', 'E', '+', '0', '1']).")
                .assertSuccess()
                .variable("X", isFloat(3.3e+01))
                .andWhen("?- number_chars(33.0, ['3', '.', '3', 'E', '+', '0', '1']).")
                .assertSuccess()
                .andWhen("?- number_chars(-33.0, ['-','3', '.', '3', 'E', '+', '0', '1']).")
                .assertSuccess()
                .andWhen("?- number_chars(33.0, Y).")
                .assertSuccess()
                .variable("Y", isList(isAtom("3"), isAtom("3"), isAtom("."),
                        isAtom("0"), isAtom("0"), isAtom("0"), isAtom("0")))
        ;

    }

    @Test
    public void testNumberCharsBadNumber() {
        assertThrows(PrologSyntaxError.class, () -> {
            given()
                    .when("?- number_chars(X, ['3', ' ']).");
        });

    }

    @Test
    public void testNumberCodes() {
        given()
                .when("?- number_codes(33, [0'3, X]).")
                .assertSuccess()
                .variable("X", isInteger('3'))
        ;
    }
}
