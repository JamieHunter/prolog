package org.jprolog.predicates;

import org.jprolog.test.Given;
import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.jprolog.test.Matchers.isInteger;

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
                .and("foo(X) :- !, '##expectLog'(X).")
                // from sec78.pl
                .and("log(X) :- '##expectLog'(X).")
                .and("twice(!) :- log('C ').")
                .and("twice(true) :- log('Moss ').")
                .and("goal((twice(_),!)).")
                .and("goal(log('Three ')).")
                .and("calls(X) :- copy_term(X, X1), call(X1).")
                .and("catches(X) :- catch(X, B, log(thrown)).")
                .and("bar(A,B) :- A=1,B=2, ! .")
                .and("bar(A,B) :- A=2,B=3 .")
        ;
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
                    .variable("X", Matchers.isAtom("sam"));


    }

    @Test
    public void testEmptyList() {
        given().when("?- foo([]).")
                .assertSuccess()
                .expectLog(Matchers.isAtom("x_empty"));
    }

    @Test
    public void testSingleEntryNotList() {
        given().when("?- foo(ping).")
                .assertSuccess()
                .expectLog(Matchers.isAtom("ping"));
    }

    @Test
    public void testSingleEntryInList() {
        given().when("?- foo([ping]).")
                .assertSuccess()
                .expectLog(
                        Matchers.isAtom("x_final"),
                        Matchers.isAtom("ping"),
                        Matchers.isAtom("z_final")
                );
    }

    @Test
    public void testMultiEntryInList() {
        given().when("?- foo([bing,bang,boom]).")
                .assertSuccess()
                .expectLog(
                        Matchers.isAtom("x_split"), // [bing,bang,boom]
                        Matchers.isAtom("bing"),
                        Matchers.isAtom("y_split"), // [bing,bang,boom]
                        Matchers.isAtom("x_split"), // [bang,boom]
                        Matchers.isAtom("bang"),
                        Matchers.isAtom("y_split"), // [bang,boom]
                        Matchers.isAtom("x_final"), // [boom]
                        Matchers.isAtom("boom"),
                        Matchers.isAtom("z_final"), // [boom]
                        Matchers.isAtom("z_split"), // [bang,boom]
                        Matchers.isAtom("z_split")  // [bing,bang,boom]
                );
    }

    @Test
    public void testMultiEntryInListWithFail() {
        // if cut is working correctly, we should get x_split, x_fail
        // one scenario with a bug is that this succeeds to try other clauses, see also testTailCut below.
        given().when("?- foo([fail,bang,boom]).")
                .assertFailed()
                .expectLog(
                        Matchers.isAtom("x_split"), // [fail,bang,boom]
                        Matchers.isAtom("x_fail")
                );
    }

    @Test
    public void testTailCut() {
        // based off of a failing Inria test
        given().when("?- bar(A,B).")
                .solutions(
                  soln -> {
                      soln.variable("A", isInteger(1));
                      soln.variable("B", isInteger(2));
                  }
                );
    }

    @Test
    public void testSec78_C_Forwards_Moss_Forwards() {
        given().when("?- twice(X), call(X), log('Forwards '), fail.")
                .assertFailed()
                .expectLog(
                        Matchers.isAtom("C "),
                        Matchers.isAtom("Forwards "),
                        Matchers.isAtom("Moss "),
                        Matchers.isAtom("Forwards ")
                );
        given().when("?- call((twice(X), call(X), log('Forwards '), fail)).")
                .assertFailed()
                .expectLog(
                        Matchers.isAtom("C "),
                        Matchers.isAtom("Forwards "),
                        Matchers.isAtom("Moss "),
                        Matchers.isAtom("Forwards ")
                );
    }

    @Test
    public void testSec78_C_Forwards_Three_Forwards() {
        given().when("?- goal(X), call(X), log('Forwards '), fail.")
                .assertFailed()
                .expectLog(
                        Matchers.isAtom("C "),
                        Matchers.isAtom("Forwards "),
                        Matchers.isAtom("Three "),
                        Matchers.isAtom("Forwards ")
                );
        given().when("?- call((goal(X), call(X), log('Forwards '), fail)).")
                .assertFailed()
                .expectLog(
                        Matchers.isAtom("C "),
                        Matchers.isAtom("Forwards "),
                        Matchers.isAtom("Three "),
                        Matchers.isAtom("Forwards ")
                );
        given().when("?- calls((goal(X), call(X), log('Forwards '), fail)).")
                .assertFailed()
                .expectLog(
                        Matchers.isAtom("C "),
                        Matchers.isAtom("Forwards "),
                        Matchers.isAtom("Three "),
                        Matchers.isAtom("Forwards ")
                );
    }

    @Test
    public void testSec78_C_Forwards_Moss_Forwards_NotNot() {
        given().and("test :- catches((twice(_), \\+(\\+(!)), log('Forwards '), fail)).")
                .when("?- test.")
                .assertFailed()
                .expectLog(
                        Matchers.isAtom("C "),
                        Matchers.isAtom("Forwards "),
                        Matchers.isAtom("Moss "),
                        Matchers.isAtom("Forwards ")
                );
    }

}
