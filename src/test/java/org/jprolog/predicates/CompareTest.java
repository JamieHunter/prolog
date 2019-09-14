package org.jprolog.predicates;

import org.jprolog.test.Given;
import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

public class CompareTest {

    protected Given reigns() {
        // From "Programming in Prolog"
        return PrologTest.given("reigns(rhodri, 844, 878).")
                .and("reigns(anarawd, 878, 916).")
                .and("reigns(hywel_dda, 916, 950).")
                .and("reigns(lago_ap_idwal, 950, 979).")
                .and("reigns(hywel_ap_ieuaf, 979, 985).")
                .and("reigns(cadwallon, 985, 986).")
                .and("reigns(maredudd, 986, 999).")
                .and("prince(X, Y) :- reigns(X, A, B), Y >= A, Y =< B.");
    }

    @Test
    public void testSimpleEquals() {
        PrologTest.given().when("?- 1 =:= 1 .")
                .assertSuccess();
        PrologTest.given().when("?- X=1, 1 =:= X .")
                .assertSuccess();
        PrologTest.given().when("?- X=1, X =:= 1 .")
                .assertSuccess();
        PrologTest.given().when("?- X=2, X =:= 1 .")
                .assertFailed();
    }

    @Test
    public void testSimpleNotEquals() {
        PrologTest.given().when("?- 2 =\\= 1 .")
                .assertSuccess();
        PrologTest.given().when("?- X=2, 1 =\\= X .")
                .assertSuccess();
        PrologTest.given().when("?- X=2, X =\\= 1 .")
                .assertSuccess();
        PrologTest.given().when("?- X=1, X =\\= 1 .")
                .assertFailed();
    }

    @Test
    public void testReigns() {
        reigns()
                .when("?- prince(cadwallon, 986).")
                .assertSuccess();
        reigns()
                .when("?- prince(rhodri, 1979).")
                .assertFailed();
    }

    @Test
    public void testReignsSubstitute() {
        reigns()
                .when("?- prince(X, 900).")
                .variable("X", Matchers.isAtom("anarawd"));
    }
}
