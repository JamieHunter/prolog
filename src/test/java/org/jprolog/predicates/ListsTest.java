package org.jprolog.predicates;

import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

/**
 * Test list manipulation predicates
 */
public class ListsTest {

    @Test
    public void testLength() {
        PrologTest.given()
                .when("?- length([a,b,c,d], Len).")
                .assertSuccess()
                .variable("Len", Matchers.isInteger(4));
        PrologTest.given()
                .when("?- length([a,b,c,d], 4).")
                .assertSuccess();
        PrologTest.given()
                .when("?- length([a,b,c,d], 5).")
                .assertFailed();
        PrologTest.given()
                .when("?- length(X, 2).")
                .assertSuccess()
                .variable("X",
                        Matchers.isList(Matchers.isUninstantiated(), Matchers.isUninstantiated()));
    }

    @Test
    public void testUnivStructToList() {
        PrologTest.given()
                .when("?- paid(today,tomorrow) =.. X.")
                .assertSuccess()
                .variable("X",
                        Matchers.isList(Matchers.isAtom("paid"), Matchers.isAtom("today"), Matchers.isAtom("tomorrow")));
        PrologTest.given()
                .when("?- paid =.. X.")
                .assertSuccess()
                .variable("X",
                        Matchers.isList(Matchers.isAtom("paid")));
    }

    @Test
    public void testUnivListToStruct() {
        PrologTest.given()
                .when("?- X =.. [one, two, three].")
                .assertSuccess()
                .variable("X",
                        Matchers.isCompoundTerm(
                                "one", Matchers.isAtom("two"), Matchers.isAtom("three")));
        PrologTest.given()
                .when("?- X =.. [hello].")
                .assertSuccess()
                .variable("X",
                        Matchers.isAtom("hello"));
    }

    @Test
    public void testUnivBiDir() {
        PrologTest.given()
                .when("?- S=one(X), L=[Y, two], S =.. L.")
                .assertSuccess()
                .variable("X", Matchers.isAtom("two"))
                .variable("Y", Matchers.isAtom("one"))
                .variable("S", Matchers.isCompoundTerm("one", Matchers.isAtom("two")))
                .variable("L", Matchers.isList(Matchers.isAtom("one"), Matchers.isAtom("two")));
    }

    @Test
    public void testMemberToVarWithBacktracking() {
        PrologTest.given()
                .when("?- member(X, [1, 2, 3]).")
                .assertSuccess()
                .variable("X", Matchers.isInteger(1))
                .anotherSolution()
                .assertSuccess()
                .variable("X", Matchers.isInteger(2))
                .anotherSolution()
                .assertSuccess()
                .variable("X", Matchers.isInteger(3))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testMemberUnifyWithBacktracking() {
        PrologTest.given()
                .when("?- member(p(X, 2), [p(3, 1), p(4, 2), p(5, Y)]).")
                .assertSuccess()
                .variable("X", Matchers.isInteger(4))
                .variable("Y", Matchers.isUninstantiated())
                .anotherSolution()
                .assertSuccess()
                .variable("X", Matchers.isInteger(5))
                .variable("Y", Matchers.isInteger(2))
                .anotherSolution()
                .assertFailed();
    }

}
