package prolog.predicates;

import org.junit.Test;
import prolog.test.Given;
import prolog.test.PrologTest;

import static prolog.test.Matchers.*;

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
                        isCompoundTerm("foo",
                            isCompoundTerm("foo",
                                    isCompoundTerm("foo",
                                        isInteger(1)),
                                        isInteger(2)),
                                    isInteger(3)))
                ;
    }

    @Test
    public void testCurrentOperatorConstrained() {
        given()
                .when("?- current_op(P, fy, foo).")
                .assertSuccess()
                .variable("P", isInteger(100))
                .anotherSolution()
                .assertFailed();
        given()
                .when("?- current_op(P, T, foo).")
                .assertSuccess()
                .variable("P", isInteger(100))
                .variable("T", isAtom("fy"))
                .anotherSolution()
                .assertSuccess()
                .variable("P", isInteger(100))
                .variable("T", isAtom("yfx"))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testCurrentOperatorUnconstrained() {
        given()
                .when("?- current_op(P, T, N), N = foo.")
                .assertSuccess()
                .variable("P", isInteger(100))
                .variable("T", isAtom("fy"))
                .variable("N", isAtom("foo"))
                .anotherSolution()
                .assertSuccess()
                .variable("P", isInteger(100))
                .variable("T", isAtom("yfx"))
                .variable("N", isAtom("foo"))
                .anotherSolution()
                .assertFailed();
    }
}
