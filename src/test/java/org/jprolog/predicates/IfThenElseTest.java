package org.jprolog.predicates;

import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

import static org.jprolog.test.Matchers.isInteger;

public class IfThenElseTest {

    //
    // Not provable indirectly tests the primary if/then/else instruction

    @Test
    public void testNotProvableSimpleCases() {
        PrologTest.given()
                .when("?- \\+ true.")
                .assertFailed();
        PrologTest.given()
                .when("?- \\+ false.")
                .assertSuccess();
    }

    @Test
    public void testNotProvableWithBacktracking() {
        PrologTest.
                given("count_parents(adam,0).")
                .and("count_parents(X,2) :- \\+(X=adam), \\+(X=eve).")
                .and("count_parents(eve,0).")
                .when("?- count_parents(jack, N).")
                .assertSuccess()
                .variable("N", Matchers.isInteger(2))
                .andWhen("?- count_parents(eve, M).")
                .assertSuccess()
                .variable("M", Matchers.isInteger(0))
                .andWhen("?- count_parents(adam, P).")
                .assertSuccess()
                .variable("P", Matchers.isInteger(0));
    }

    @Test
    public void testNotProvableIndirectWithBacktracking() {
        PrologTest.given()
                .when("?- Q=true, \\+Q.")
                .assertFailed();
        PrologTest.given()
                .when("?- Q=false, \\+Q.")
                .assertSuccess();
    }

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
                .expectLog(Matchers.isInteger(1), Matchers.isInteger(2), null)
                // Also succeeds, instantiates X
                .andWhen("?- a(X) -> '##expectLog'(X).")
                .assertSuccess()
                .expectLog(Matchers.isInteger(1), Matchers.isInteger(1), null)
                // decision point for b, not for a.
                .andWhen("?- a(X) -> (b(Y), false).")
                .assertFailed()
                .expectLog(Matchers.isInteger(1), Matchers.isInteger(3), Matchers.isInteger(4), null);

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
                .expectLog(Matchers.isInteger(2), null)
                // Succeeds true, succeeds a(1), therefore skips a(2)
                .andWhen("?- true -> a(1) ; a(2).")
                .assertSuccess()
                .expectLog(Matchers.isInteger(1), null)

                // Succeeds, instantiates X, Y, not Z
                .andWhen("?- a(X) -> b(Y) ; c(Z).")
                .assertSuccess()
                .variable("X", Matchers.isInteger(1))
                .variable("Y", Matchers.isInteger(3))
                .variable("Z", Matchers.isUninstantiated())
                .expectLog(Matchers.isInteger(1), Matchers.isInteger(3), null)
                // Force backtracking (1)
                .anotherSolution()
                .assertSuccess()
                .variable("X", Matchers.isInteger(1))
                .variable("Y", Matchers.isInteger(4))
                .variable("Z", Matchers.isUninstantiated())
                .expectLog(Matchers.isInteger(4), null)
                // Force backtracking (2)
                .anotherSolution()
                .assertFailed()
                .expectLog()

                // Fails, instantiates Z
                .andWhen("?- false -> b(Y) ; c(Z).")
                .assertSuccess()
                .variable("Y", Matchers.isUninstantiated())
                .variable("Z", Matchers.isInteger(5))
                .expectLog(Matchers.isInteger(5), null)
                // Force backtracking (1)
                .anotherSolution()
                .assertSuccess()
                .variable("Y", Matchers.isUninstantiated())
                .variable("Z", Matchers.isInteger(6))
                .expectLog(Matchers.isInteger(6), null)
                // Force backtracking (2)
                .anotherSolution()
                .assertFailed()
                .expectLog()
        ;
    }
}
