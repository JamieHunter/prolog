package prolog.predicates;

import org.junit.jupiter.api.Test;
import prolog.test.PrologTest;

import static prolog.test.Matchers.*;

/**
 * Test term manipulation predicates
 */
public class TermsTest {

    @Test
    public void testArg() {
        PrologTest.given()
                .when("?- arg(2,father(_,cain),Z).")
                .assertSuccess()
                .variable("Z", isAtom("cain"));
    }

    @Test
    public void testFunctor() {
        PrologTest.given()
                .when("?- functor(father(_,_),Functor,Lambda).")
                .assertSuccess()
                .variable("Functor", isAtom("father"))
                .variable("Lambda", isInteger(2));
        PrologTest.given()
                .when("?- functor(father,Functor,Lambda).")
                .assertSuccess()
                .variable("Functor", isAtom("father"))
                .variable("Lambda", isInteger(0));
        PrologTest.given()
                .when("?- functor(son(2),son,1).")
                .assertSuccess();

    }

    @Test
    public void testAtom() {
        PrologTest.given()
                .when("?- atom(foo).")
                .assertSuccess();
        PrologTest.given()
                .when("?- atom(Foo).")
                .assertFailed();
        PrologTest.given()
                .when("?- atom(123).")
                .assertFailed();
        PrologTest.given()
                .when("?- atom(\"abc\").")
                .assertFailed();
        PrologTest.given()
                .when("?- atom(a(b,c)).")
                .assertFailed();
    }

    @Test
    public void testAtomic() {
        PrologTest.given()
                .when("?- atomic(foo).")
                .assertSuccess();
        PrologTest.given()
                .when("?- atomic(Foo).")
                .assertFailed();
        PrologTest.given()
                .when("?- atomic(123).")
                .assertSuccess();
        PrologTest.given()
                .when("?- atomic(\"abc\").")
                .assertSuccess();
        PrologTest.given()
                .when("?- atomic(a(b,c)).")
                .assertFailed();
    }

    @Test
    public void testInteger() {
        PrologTest.given()
                .when("?- integer(123).")
                .assertSuccess();
        PrologTest.given()
                .when("?- integer(123.45).")
                .assertFailed();
        PrologTest.given()
                .when("?- integer(abc).")
                .assertFailed();
    }

    @Test
    public void testFloat() {
        PrologTest.given()
                .when("?- float(123.45).")
                .assertSuccess();
        PrologTest.given()
                .when("?- float(123).")
                .assertFailed();
        PrologTest.given()
                .when("?- float(abc).")
                .assertFailed();
    }

    @Test
    public void testNumber() {
        PrologTest.given()
                .when("?- number(123.45).")
                .assertSuccess();
        PrologTest.given()
                .when("?- number(123).")
                .assertSuccess();
        PrologTest.given()
                .when("?- number(abc).")
                .assertFailed();
    }

    @Test
    public void testString() {
        PrologTest.given()
                .when("?- string(\"abc\").")
                .assertSuccess();
        PrologTest.given()
                .when("?- string(`abc`).")
                .assertFailed(); // it's a list
        PrologTest.given()
                .when("?- string(123.45).")
                .assertFailed();
        PrologTest.given()
                .when("?- string(123).")
                .assertFailed();
        PrologTest.given()
                .when("?- string(abc).")
                .assertFailed();
    }

    @Test
    public void testVar() {
        PrologTest.given()
                .when("?- var(X).")
                .assertSuccess();
        PrologTest.given()
                .when("?- X=5, var(X).")
                .assertFailed();
    }

    @Test
    public void testNonVar() {
        PrologTest.given()
                .when("?- nonvar(X).")
                .assertFailed();
        PrologTest.given()
                .when("?- X=5, nonvar(X).")
                .assertSuccess();
    }
}
