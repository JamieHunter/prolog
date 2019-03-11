package prolog.library;

import org.junit.Test;
import prolog.test.Given;
import prolog.test.PrologTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static prolog.test.Matchers.*;

public class ThrowCatchTest {

    private Given given() {
        return PrologTest.given("p1 :- true.")
                .and("p1 :- throw(b).")
                .and("p2 :- throw(b).")
                .and("r(X) :- throw(X).")
                .and("q :- catch(p, B, write('hellop')), r(c).");

    }

    @Test
    public void testThrowCatchCustom() {
        given().when("?- catch(p1, X, (Y = X)).")
                .assertSuccess()
                .variable("X", isUninstantiated())
                .variable("Y", isUninstantiated());
        given().when("?- catch(p2, X, (Y = X)).")
                .assertSuccess()
                .variable("X", isAtom("b"))
                .variable("Y", isAtom("b"));
    }
}
