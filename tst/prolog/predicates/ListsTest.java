package prolog.predicates;

import org.junit.Test;
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
                        isList(isAnonymousVariable(), isAnonymousVariable()));
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

}
