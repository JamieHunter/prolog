package org.jprolog.predicates;

import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

import static org.jprolog.test.Matchers.isInteger;

public class NotProvableTest {

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
    public void notProbableWithOr() {
        PrologTest.given()
                .when("?- (A=1;A=2),\\+ (!,fail).")
                .solutions(
                        then -> then.variable("A", isInteger(1)),
                        then -> then.variable("A", isInteger(2))
                );
    }
}
