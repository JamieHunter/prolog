package prolog.predicates;

import org.junit.Test;
import prolog.test.PrologTest;

import static org.junit.Assert.fail;
import static prolog.test.Matchers.isInteger;
import static prolog.test.Matchers.isUninstantiated;

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
                .variable("N", isInteger(2))
                .andWhen("?- count_parents(eve, M).")
                .assertSuccess()
                .variable("M", isInteger(0))
                .andWhen("?- count_parents(adam, P).")
                .assertSuccess()
                .variable("P", isInteger(0));
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
                .assertSuccess()
                .variable("X", isInteger(1))
                .variable("Y", isInteger(3))
                .variable("Z", isUninstantiated())
                .expectLog(isInteger(1), isInteger(3), null)
                // Force backtracking (1)
                .anotherSolution()
                .assertSuccess()
                .variable("X", isInteger(1))
                .variable("Y", isInteger(4))
                .variable("Z", isUninstantiated())
                .expectLog(isInteger(4), null)
                // Force backtracking (2)
                .anotherSolution()
                .assertFailed()
                .expectLog()

                // Fails, instantiates Z
                .andWhen("?- false -> b(Y) ; c(Z).")
                .assertSuccess()
                .variable("Y", isUninstantiated())
                .variable("Z", isInteger(5))
                .expectLog(isInteger(5), null)
                // Force backtracking (1)
                .anotherSolution()
                .assertSuccess()
                .variable("Y", isUninstantiated())
                .variable("Z", isInteger(6))
                .expectLog(isInteger(6), null)
                // Force backtracking (2)
                .anotherSolution()
                .assertFailed()
                .expectLog()
                ;
    }
}
