package prolog.execution;

import org.junit.jupiter.api.Test;
import prolog.test.PrologTest;

/**
 * Example recursion
 */
public class RecursionTest {

    @Test
    public void testMemberOf() {
        PrologTest.given("my_member(X, [X|_]).")
                .and("my_member(X, [_|Y]) :- my_member(X, Y).") // tail-call
                .when("?- my_member(a, [a, b, c]).")
                .assertSuccess()
                .andWhen("?- my_member(b, [a, b, c]).")
                .assertSuccess()
                .andWhen("?- my_member(c, [a, b, c]).")
                .assertSuccess();
    }

    @Test
    public void testMemberOfWithCall() {
        // ensure backtracking occurs through a 'call'.
        PrologTest.
                given("my_member(X, [X|_]).")
                .and("indirect(Q) :- call(Q).")
                .and("my_member(X, [_|Y]) :- indirect(my_member(X, Y)).")
                .when("?- my_member(b, [a, b, c]).")
                .assertSuccess();
    }
}
