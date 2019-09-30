package org.jprolog.predicates;

import org.jprolog.test.Given;
import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

public class OpTest {

    Given given() {
        return PrologTest.
                given("?- op(100, fy, foo).")
                .and("?- op(100, yfx, foo).")
                ;
    }

    @Test
    public void testCustomOperator() {
        given()
                .when("?- X = foo 1 foo 2 foo 3.")
                .assertSuccess()
                .variable("X",
                        Matchers.isCompoundTerm("foo",
                            Matchers.isCompoundTerm("foo",
                                    Matchers.isCompoundTerm("foo",
                                        Matchers.isInteger(1)),
                                        Matchers.isInteger(2)),
                                    Matchers.isInteger(3)))
                ;
    }

    @Test
    public void testCustomOperatorDelete() {
        given()
                .when("?- current_op(P, fy, foo).")
                .assertSuccess()
                .andWhen("?- op(0, fy, foo).")
                .assertSuccess()
                .andWhen("?- current_op(P, fy, foo).")
                .assertFailed()
        ;
    }

    @Test
    public void testCurrentOperatorConstrained() {
        given()
                .when("?- current_op(P, fy, foo).")
                .assertSuccess()
                .variable("P", Matchers.isInteger(100))
                .anotherSolution()
                .assertFailed();
        given()
                .when("?- current_op(P, T, foo).")
                .assertSuccess()
                .variable("P", Matchers.isInteger(100))
                .variable("T", Matchers.isAtom("fy"))
                .anotherSolution()
                .assertSuccess()
                .variable("P", Matchers.isInteger(100))
                .variable("T", Matchers.isAtom("yfx"))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testCurrentOperatorUnconstrained() {
        given()
                .when("?- current_op(P, T, N), N = foo.")
                .assertSuccess()
                .variable("P", Matchers.isInteger(100))
                .variable("T", Matchers.isAtom("fy"))
                .variable("N", Matchers.isAtom("foo"))
                .anotherSolution()
                .assertSuccess()
                .variable("P", Matchers.isInteger(100))
                .variable("T", Matchers.isAtom("yfx"))
                .variable("N", Matchers.isAtom("foo"))
                .anotherSolution()
                .assertFailed();
    }
}
