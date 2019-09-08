package prolog.predicates;

import org.junit.jupiter.api.Test;
import prolog.test.PrologTest;

import static prolog.test.Matchers.*;

/**
 * Test list manipulation predicates
 */
public class ListsTest {

    @Test
    public void testLength() {
        PrologTest.given()
                .when("?- length([a,b,c,d], Len).")
                .assertSuccess()
                .variable("Len", isInteger(4));
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
                        isList(isUninstantiated(), isUninstantiated()));
    }

    @Test
    public void testUnivStructToList() {
        PrologTest.given()
                .when("?- paid(today,tomorrow) =.. X.")
                .assertSuccess()
                .variable("X",
                        isList(isAtom("paid"), isAtom("today"), isAtom("tomorrow")));
        PrologTest.given()
                .when("?- paid =.. X.")
                .assertSuccess()
                .variable("X",
                        isList(isAtom("paid")));
    }

    @Test
    public void testUnivListToStruct() {
        PrologTest.given()
                .when("?- X =.. [one, two, three].")
                .assertSuccess()
                .variable("X",
                        isCompoundTerm(
                                "one", isAtom("two"), isAtom("three")));
        PrologTest.given()
                .when("?- X =.. [hello].")
                .assertSuccess()
                .variable("X",
                        isAtom("hello"));
    }

    @Test
    public void testUnivBiDir() {
        PrologTest.given()
                .when("?- S=one(X), L=[Y, two], S =.. L.")
                .assertSuccess()
                .variable("X", isAtom("two"))
                .variable("Y", isAtom("one"))
                .variable("S", isCompoundTerm("one", isAtom("two")))
                .variable("L", isList(isAtom("one"), isAtom("two")));
    }

    @Test
    public void testMemberToVarWithBacktracking() {
        PrologTest.given()
                .when("?- member(X, [1, 2, 3]).")
                .assertSuccess()
                .variable("X", isInteger(1))
                .anotherSolution()
                .assertSuccess()
                .variable("X", isInteger(2))
                .anotherSolution()
                .assertSuccess()
                .variable("X", isInteger(3))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testMemberUnifyWithBacktracking() {
        PrologTest.given()
                .when("?- member(p(X, 2), [p(3, 1), p(4, 2), p(5, Y)]).")
                .assertSuccess()
                .variable("X", isInteger(4))
                .variable("Y", isUninstantiated())
                .anotherSolution()
                .assertSuccess()
                .variable("X", isInteger(5))
                .variable("Y", isInteger(2))
                .anotherSolution()
                .assertFailed();
    }

}
