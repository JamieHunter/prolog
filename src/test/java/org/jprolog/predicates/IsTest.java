package org.jprolog.predicates;

import org.jprolog.exceptions.PrologEvaluationError;
import org.jprolog.exceptions.PrologInstantiationError;
import org.jprolog.exceptions.PrologTypeError;
import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test various is expressions.
 */
public class IsTest {

    @Test
    public void testIsAdd() {
        PrologTest.given().when("?- X is 1+2 .")
                .assertSuccess()
                .variable("X", Matchers.isInteger(3));
    }

    @Test
    public void testIsAddMixed() {
        PrologTest.given().when("?- X is 3 + 4.5 .")
                .assertSuccess()
                .variable("X", Matchers.isFloat(closeTo(7.5, 0.001)));
    }

    @Test
    public void testIsMultiplyWithVar() {
        PrologTest.given().when("?- Y=6, X is 5*Y .")
                .assertSuccess()
                .variable("Y", Matchers.isInteger(6))
                .variable("X", Matchers.isInteger(30));
    }

    @Test
    public void testIsIntDivide() {
        PrologTest.given().when("?- Y=20, X is Y // 4 .")
                .assertSuccess()
                .variable("Y", Matchers.isInteger(20))
                .variable("X", Matchers.isInteger(5));
    }

    @Test
    public void testIsFloatDivide() {
        PrologTest.given().when("?- Y=20, X is Y / 4 .")
                .assertSuccess()
                .variable("Y", Matchers.isInteger(20))
                .variable("X", Matchers.isFloat(closeTo(5.0, 0.0001)));
    }

    @Test
    public void testIsMultiplyUndefVar() {
        assertThrows(PrologInstantiationError.class, () -> {
            PrologTest.given().when("?- X is 5*Y .");
        });
    }

    @Test
    public void testIsMultiplyAtomVar() {
        assertThrows(PrologTypeError.class, () -> {
            PrologTest.given().when("?- X is foo .");
        });
    }

    @Test
    public void testIsIntDivideByZero() {
        assertThrows(PrologEvaluationError.class, () -> {
            PrologTest.given().when("?- X is 1//0 .");
        });
    }

    @Test
    public void testIsFloatDivideByZero() {
        assertThrows(PrologEvaluationError.class, () -> {
            PrologTest.given().when("?- X is 1.0/0.0 .");
        });
    }

    @Test
    public void testIsIntMod() {
        PrologTest.given().when("?- Y=20, X is Y mod 3 .")
                .assertSuccess()
                .variable("Y", Matchers.isInteger(20))
                .variable("X", Matchers.isInteger(2));
    }

    @Test
    public void testIsAbs() {
        PrologTest.given().when("?- X is abs(10-1).")
                .assertSuccess()
                .variable("X", Matchers.isInteger(9));
    }

    @Test
    public void testIsSign() {
        PrologTest.given().when("?- X is sign(10-1).")
                .assertSuccess()
                .variable("X", Matchers.isInteger(1));
    }

    @Test
    public void testRecursiveExpression() {
        PrologTest.given().when("?- (X = ((1 + 2)), 'is'(Y, ((X)) * 3)).")
                .assertSuccess()
                .variable("X", Matchers.isCompoundTerm("+", Matchers.isInteger(1), Matchers.isInteger(2)))
                .variable("Y", Matchers.isInteger(9));
    }
}
