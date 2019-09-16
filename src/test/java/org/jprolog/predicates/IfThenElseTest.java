package org.jprolog.predicates;

import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

import static org.jprolog.test.Matchers.isInteger;

public class IfThenElseTest {

    @Test
    @SuppressWarnings("unchecked")
    public void ifThenSimple() {
        PrologTest.given("a(1) :- '##expectLog'(1).")
                .and("a(2) :- '##expectLog'(2).")
                .and("b(3) :- '##expectLog'(3).")
                .and("b(4) :- '##expectLog'(4).")
                // Fails before getting to a(1)
                .when("?- (false -> a(1)),a(2).")
                .assertFailed()
                .expectLog()
                // Succeeds both
                .andWhen("?- (true -> a(1)), a(2).")
                .assertSuccess()
                .expectLog(isInteger(1), isInteger(2), null)
                // Also succeeds, instantiates X
                .andWhen("?- a(X) -> '##expectLog'(X).")
                .assertSuccess()
                .expectLog(isInteger(1), isInteger(1), null)
                // decision point for b, not for a.
                .andWhen("?- a(X) -> (b(Y), false).")
                .assertFailed()
                .expectLog(isInteger(1), isInteger(3), isInteger(4), null);

    }

    @Test
    public void ifThenElseWithOr() {
        PrologTest.given()
                .when("?- true->(A=1;A=2);true.")
                .solutions(
                        then -> then.variable("A", isInteger(1)),
                        then -> then.variable("A", isInteger(2))
                );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void ifThenElse() {
        PrologTest.given("a(1) :- '##expectLog'(1).")
                .and("a(2) :- '##expectLog'(2).")
                .and("b(3) :- '##expectLog'(3).")
                .and("b(4) :- '##expectLog'(4).")
                .and("c(5) :- '##expectLog'(5).")
                .and("c(6) :- '##expectLog'(6).")
                // Fails before getting to a(1), therefore reaches a(2)
                .when("?- false -> a(1) ; a(2).")
                .assertSuccess()
                .expectLog(isInteger(2), null)
                // Succeeds true, succeeds a(1), therefore skips a(2)
                .andWhen("?- true -> a(1) ; a(2).")
                .assertSuccess()
                .expectLog(isInteger(1), null)

                // Succeeds, instantiates X, Y, not Z
                .andWhen("?- a(X) -> b(Y) ; c(Z).")
                .solutions(then -> {
                            then.variable("X", isInteger(1))
                                    .variable("Y", isInteger(3))
                                    .variable("Z", Matchers.isUninstantiated())
                                    .expectLog(isInteger(1), isInteger(3), null);

                        },
                        then -> {
                            then.variable("X", isInteger(1))
                                    .variable("Y", isInteger(4))
                                    .variable("Z", Matchers.isUninstantiated())
                                    .expectLog(isInteger(4), null);
                        })
                .expectLog()

                // Fails, instantiates Z
                .andWhen("?- false -> b(Y) ; c(Z).")
                .assertSuccess()
                .variable("Y", Matchers.isUninstantiated())
                .variable("Z", isInteger(5))
                .expectLog(isInteger(5), null)
                // Force backtracking (1)
                .anotherSolution()
                .assertSuccess()
                .variable("Y", Matchers.isUninstantiated())
                .variable("Z", isInteger(6))
                .expectLog(isInteger(6), null)
                // Force backtracking (2)
                .anotherSolution()
                .assertFailed()
                .expectLog()
        ;
    }
}
