package prolog.execution;

import org.junit.Test;
import prolog.expressions.Term;
import prolog.test.PrologTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static prolog.test.Matchers.*;

public class BacktrackingTest {
    @Test
    public void testSimpleChoicePoint() {
        PrologTest.given().when("?- X=1, false; Y=1; Z=1.")
                .assertSuccess()
                .variable("X", isUninstantiated())
                .variable("Y", isInteger(1))
                .variable("Z", isUninstantiated());
    }

    @Test
    public void clauseSimpleChoicePoint() {
        PrologTest.given("a(1) :- false.")
                .and("a(2) :- true.")
                .and("a(3) :- false.")
                .when("?- a(X).")
                .assertSuccess()
                .variable("X", isInteger(2));
    }

    @Test
    public void clauseNestedChoicePoint() {
        PrologTest.given("a(1).")
                .and("a(2).")
                .and("a(3).")
                .and("b(N) :- a(N), N >= 2.")
                .when("?- b(X).")
                .assertSuccess()
                .variable("X", isInteger(2));
    }

    @Test
    public void clauseDoubleNestedChoicePointTail() {
        PrologTest.given("a(1).")
                .and("a(2).")
                .and("a(3).")
                .and("a(4).")
                .and("b(N) :- a(N), N >= 2.")
                .and("c(M) :- b(M), M >= 3.")
                .when("?- c(X).")
                .assertSuccess()
                .variable("X", isInteger(3));
    }

    @Test
    public void clauseTailChoicePoint() {
        PrologTest.given("a(1).")
                .and("a(2).")
                .and("a(3).")
                .and("b(1, M) :- a(M).")
                .and("b(N, M) :- NN is N-1, a(N), b(NN,M).")
                .when("?- b(3, 2).")
                .assertSuccess();
    }

}
