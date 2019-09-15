package org.jprolog.predicates;

import org.jprolog.test.Given;
import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class CharConversionTest {

    Given given() {
        return PrologTest.
                given("?- char_conversion(a, b).")
                .and("?- char_conversion(b, x).")
                ;
    }

    @Disabled("conversion test not yet implemented")
    @Test
    public void testParserConversion() {
        // TODO (currently tested via Prolog Vanilla test)
    }

    @Test
    public void testCharConversionScanSingle() {
        given()
                .when("?- current_char_conversion(a, b).")
                .assertSuccess()
                .andWhen("?- current_char_conversion(a, a).")
                .assertFailed()
                .andWhen("?- current_char_conversion(b, x).")
                .assertSuccess()
                .andWhen("?- current_char_conversion(x, x).")
                .assertSuccess()
                .anotherSolution()
                .assertFailed();

    }

    @Test
    public void testCharConversionScanSingleUnify() {
        given()
                .when("?- current_char_conversion(a, X).")
                .assertSuccess()
                .variable("X", Matchers.isAtom("b"))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testCharConversionScanMultipleUnify() {
        given()
                .when("?- current_char_conversion(X, x).")
                .assertSuccess()
                .variable("X", Matchers.isAtom("b"))
                .anotherSolution()
                .assertSuccess()
                .variable("X", Matchers.isAtom("x"))
                .anotherSolution()
                .assertFailed();
    }
}
