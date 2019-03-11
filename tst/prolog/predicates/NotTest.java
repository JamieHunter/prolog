package prolog.predicates;

import org.junit.Test;
import prolog.test.PrologTest;

import static org.junit.Assert.fail;
import static prolog.test.Matchers.isInteger;

public class NotTest {

    @Test
    public void testSimpleCases() {
        PrologTest.given()
                .when("?- \\+ true.")
                .assertFailed();
        PrologTest.given()
                .when("?- \\+ false.")
                .assertSuccess();
    }

    @Test
    public void testWithBacktracking() {
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
    public void testIndirectWithBacktracking() {
        PrologTest.given()
                .when("?- Q=true, \\+Q.")
                .assertFailed();
        PrologTest.given()
                .when("?- Q=false, \\+Q.")
                .assertSuccess();
    }
}
