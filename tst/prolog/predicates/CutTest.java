package prolog.predicates;

import org.junit.Test;
import prolog.test.Given;
import prolog.test.PrologTest;

import static prolog.test.Matchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Verify correct cut behavior across predicate boundaries
 * See also {@link TailCallRecursionTest}
 */
public class CutTest {

    protected Given given() {
        // Note that below intentionally prevents tail-call optimization
        return PrologTest
                .given("foo([]) :- !, '##expectLog'(x_empty).")
                .and("foo([H]) :- !, '##expectLog'(x_final), foo(H), '##expectLog'(z_final).")
                .and("foo([H|T]) :- !, '##expectLog'(x_split), foo(H), '##expectLog'(y_split), foo(T), '##expectLog'(z_split).")
                .and("foo(fail) :- !, '##expectLog'(x_fail), fail, '##expectLog'(z_fail).")
                .and("foo(X) :- !, '##expectLog'(X).");
    }

    @Test
    public void reference() {
            // this relies on non-cut behavior, so serves as a reference
            PrologTest.given("old(X) :- age(X,Y), Y > 30.")
                    .and("age(john, 10).")
                    .and("age(sam, 50).")
                    .and("age(peter, 13).")
                    .when("?- old(X).")
                    .assertSuccess()
                    .variable("X", isAtom("sam"));


    }

    @Test
    public void testEmptyList() {
        given().when("?- foo([]).")
                .assertSuccess()
                .expectLog(isAtom("x_empty"));
    }

    @Test
    public void testSingleEntryNotList() {
        given().when("?- foo(ping).")
                .assertSuccess()
                .expectLog(isAtom("ping"));
    }

    @Test
    public void testSingleEntryInList() {
        given().when("?- foo([ping]).")
                .assertSuccess()
                .expectLog(
                        isAtom("x_final"),
                        isAtom("ping"),
                        isAtom("z_final")
                );
    }

    @Test
    public void testMultiEntryInList() {
        given().when("?- foo([bing,bang,boom]).")
                .assertSuccess()
                .expectLog(
                        isAtom("x_split"), // [bing,bang,boom]
                        isAtom("bing"),
                        isAtom("y_split"), // [bing,bang,boom]
                        isAtom("x_split"), // [bang,boom]
                        isAtom("bang"),
                        isAtom("y_split"), // [bang,boom]
                        isAtom("x_final"), // [boom]
                        isAtom("boom"),
                        isAtom("z_final"), // [boom]
                        isAtom("z_split"), // [bang,boom]
                        isAtom("z_split")  // [bing,bang,boom]
                );
    }

    @Test
    public void testMultiEntryInListWithFail() {
        // if cut is working correctly, we should get x_split, x_fail
        // one scenario with a bug is that this succeeds to try other clauses
        given().when("?- foo([fail,bang,boom]).")
                .assertFailed()
                .expectLog(
                        isAtom("x_split"), // [fail,bang,boom]
                        isAtom("x_fail")
                );
    }

}
