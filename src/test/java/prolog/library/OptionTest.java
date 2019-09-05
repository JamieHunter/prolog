package prolog.library;

import org.junit.Test;
import prolog.test.PrologTest;

import static prolog.test.Matchers.*;

/**
 * Tests for option.pl (Option library), not to be confused with the built-in option processing
 */
public class OptionTest {

    @Test
    public void testOptionHelperMatchIdentity() {
        PrologTest.given()
                .when("?- select_option(o1(a), [o1(a)], [], _).")
                .assertSuccess()
        ;
        PrologTest.given()
                .when("?- select_option(o1(a), [o1(a)], []).")
                .assertSuccess()
        ;
        PrologTest.given()
                .when("?- option(o1(a), [o1(a)], _).")
                .assertSuccess()
        ;
        PrologTest.given()
                .when("?- option(o1(a), [o1(a)]).")
                .assertSuccess()
        ;
        PrologTest.given()
                .when("?- select_option(o1(a), [o1(a)], [], failed).")
                .assertSuccess()
        ;
        PrologTest.given()
                .when("?- option(o1(a), [o1(a)], failed).")
                .assertSuccess()
        ;
        PrologTest.given()
                .when("?- select_option(o1(a,b), [o1(a,b)], [], _).")
                .assertSuccess()
        ;
        PrologTest.given()
                .when("?- select_option(o1(a,b), [o1(a,b)], []).")
                .assertSuccess()
        ;
        PrologTest.given()
                .when("?- option(o1(a,b), [o1(a,b)]).")
                .assertSuccess()
        ;
        PrologTest.given()
                .when("?- select_option(o1(a,b), [o1(a,b)], [], failed).")
                .assertSuccess()
        ;
        PrologTest.given()
                .when("?- option(o1(a,b), [o1(a,b)], failed).")
                .assertSuccess()
        ;
    }

    @Test
    public void testOptionHelperMatchTypical() {
        PrologTest.given()
                .when("?- option(o1(X), [o1(a)]).")
                .assertSuccess()
                .variable("X", isAtom("a"))
        ;
        PrologTest.given()
                .when("?- option(o1(X,Y), [o1(a,b)]).")
                .assertSuccess()
                .variable("X", isAtom("a"))
                .variable("Y", isAtom("b"))
        ;
    }

    @Test
    public void testOptionHelperMatchAtypical() {
        // Legal, but atypical
        PrologTest.given()
                .when("?- option(o1(a), [o1(X)]).")
                .assertSuccess()
                .variable("X", isAtom("a"))
        ;
        PrologTest.given()
                .when("?- option(o1(a,b), [o1(X,Y)]).")
                .assertSuccess()
                .variable("X", isAtom("a"))
                .variable("Y", isAtom("b"))
        ;
    }

    @Test
    public void testOptionHelperEquals() {
        // Using the '=' alternative syntax
        PrologTest.given()
                .when("?- option(o1(a), [o1 = a]).")
                .assertSuccess()
        ;
        PrologTest.given()
                .when("?- option(o1(X), [o1 = a]).")
                .assertSuccess()
                .variable("X", isAtom("a"))
        ;
        PrologTest.given()
                .when("?- option(o1(a), [o1 = X]).")
                .assertSuccess()
                .variable("X", isAtom("a"))
        ;
        PrologTest.given()
                .when("?- option(o1(_), [o1 = X]).")
                .assertSuccess()
                .variable("X", isUninstantiated())
        ;
    }

    @Test
    public void testOptionHelperMismatch() {
        PrologTest.given()
                .when("?- select_option(o1(a), [o1(b)], [o1(b)]).")
                .assertSuccess()
        ;
        PrologTest.given()
                .when("?- select_option(o1(a), [o2(a)], [o2(a)]).")
                .assertSuccess()
        ;
        PrologTest.given()
                .when("?- select_option(o1(a), [o1=b], [o1=b]).")
                .assertSuccess()
        ;
        PrologTest.given()
                .when("?- select_option(o1(a), [o2=a], [o2=a]).")
                .assertSuccess()
        ;
        PrologTest.given()
                .when("?- select_option(o1(a), [o2=a], [o2=a]).")
                .assertSuccess()
        ;
        PrologTest.given()
                .when("?- select_option(o1(a,b), [o1(b,c)], [o1(b,c)]).")
                .assertSuccess()
        ;
        PrologTest.given()
                .when("?- select_option(o1(a,b), [o1(_)], [o1(_)]).")
                .assertSuccess()
        ;
        PrologTest.given()
                .when("?- select_option(o1(_), [o1(a,b)], [o1(a,b)]).")
                .assertSuccess()
        ;
    }

    @Test
    public void testSelectOptionExtractWithDefault() {
        PrologTest.given()
                .when("?- select_option(c(X), [], [], default).")
                .assertSuccess()
                .variable("X", isAtom("default"))
        ;
    }

    @Test
    public void testSelectOptionExtractUnifies() {
        PrologTest.given()
                .when("?- select_option(c(X), [], R, D).")
                .assertSuccess()
                // Verify R is empty list
                .variable("R", isList())
                // Verify X and D are co-reference
                .variable("D", isUninstantiated())
                .variable("X", isUninstantiated())
                .andWhen("?- select_option(c(X), [], R, D), D = true .")
                .assertSuccess()
                .variable("X", isAtom("true"))
        ;
    }

    @Test
    public void testSelectOptionExtractWithSpecifiedDefaultEmpty() {
        PrologTest.given()
                .when("?- select_option(c(X), [], R, empty).")
                .assertSuccess()
                .variable("R", isList())
                .variable("X", isAtom("empty"))
        ;
    }

    @Test
    public void testSelectOptionOfList() {
        PrologTest.given()
                .when("?- select_option(c(X), [a(1), b(2), c(3), d(4)], [a(1), b(2), d(4)], empty).")
                .assertSuccess()
                .variable("X", isInteger(3))
        ;
    }

    @Test
    public void testSelectOptionOfListEquals() {
        PrologTest.given()
                .when("?- select_option(c(X), [a = 1, b = 2, c = 3, d = 4], [a = 1, b = 2, d=4], empty).")
                .assertSuccess()
                .variable("X", isInteger(3))
        ;
    }

    @Test
    public void testSelectOptionOfListUseDefault() {
        PrologTest.given()
                .when("?- select_option(x(X), [a(1), b(2), c(3), d(4)], [a(1), b(2), c(3), d(4)], empty).")
                .assertSuccess()
                .variable("X", isAtom("empty"))
        ;
    }
}
