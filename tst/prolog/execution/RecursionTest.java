package prolog.execution;

import org.junit.Test;
import prolog.test.PrologTest;

/**
 * Example recursion
 */
public class RecursionTest {

    @Test
    public void testMemberOf() {
        PrologTest.given("member(X, [X|_]).")
                .and("member(X, [_|Y]) :- member(X, Y).") // tail-call
                .when("?- member(a, [a, b, c]).")
                .assertSuccess()
                .andWhen("?- member(b, [a, b, c]).")
                .assertSuccess()
                .andWhen("?- member(c, [a, b, c]).")
                .assertSuccess();
    }

    @Test
    public void testMemberOfWithCall() {
        // ensure backtracking occurs through a 'call'.
        PrologTest.
                given("member(X, [X|_]).")
                .and("indirect(Q) :- call(Q).")
                .and("member(X, [_|Y]) :- indirect(member(X, Y)).")
                .when("?- member(b, [a, b, c]).")
                .assertSuccess();
    }
}
