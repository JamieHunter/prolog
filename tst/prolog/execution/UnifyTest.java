package prolog.execution;

import org.junit.Test;
import prolog.expressions.Term;
import prolog.test.PrologTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static prolog.test.Matchers.*;

public class UnifyTest {

    @Test
    public void testTrivialFailures() {
        PrologTest.given()
                .when("?- a = b.")
                .assertFailed();
        PrologTest.given()
                .when("?- b = a.")
                .assertFailed();
        PrologTest.given()
                .when("?- 1 = 2.")
                .assertFailed();
        PrologTest.given()
                .when("?- 1 = b.")
                .assertFailed();
        PrologTest.given()
                .when("?- 1 = b(a).")
                .assertFailed();
        PrologTest.given()
                .when("?- \"hello\" = 2.")
                .assertFailed();
    }

    @Test
    public void testTrivialSuccess() {
        PrologTest.given()
                .when("?- a = a.")
                .assertSuccess();
        PrologTest.given()
                .when("?- 1 = 1.")
                .assertSuccess();
        PrologTest.given()
                .when("?- \"hello\" = \"hello\".")
                .assertSuccess();
    }

    @Test
    public void testInstantiateLeft() {
        PrologTest.given()
                .when("?- X = foo(1,2).")
                .assertSuccess()
                .variable("X", isCompoundTerm("foo", isInteger(1), isInteger(2)));
    }

    @Test
    public void testInstantiateRight() {
        PrologTest.given()
                .when("?- \"hello\" = Y.")
                .assertSuccess()
                .variable("Y", isString("hello"));
    }

    @Test
    public void testInstantiateCoreference() {
        PrologTest.given()
                .when("?- X = Y, X = 1.")
                .assertSuccess()
                .variable("X", isInteger(1))
                .variable("Y", isInteger(1));
    }

    @Test
    public void testDeferred() {
        PrologTest.given()
                .when("?- X = a(b,c), Y = a(b,c), X = Y.")
                .assertSuccess();
    }

    @Test
    public void testTailStructure() {
        PrologTest.given()
                .when("?- a(1, 2, b(3,4)) = a(1,2, b(3,4)).")
                .assertSuccess();
    }

    @Test
    public void testStructured() {
        PrologTest.given()
                .when("?- (1+2+3+4) = (1+2+3+4).")
                .assertSuccess();
    }

    @Test
    public void testStructured2() {
        PrologTest.given()
                .when("?- (1+2*3+4) = (1+2*3+4).")
                .assertSuccess();
    }

    @Test
    public void testStructuredFail() {
        PrologTest.given()
                .when("?- (1+2+4) = (1+2*3+4).")
                .assertFailed();
    }

    @Test
    public void testStructuredArity() {
        PrologTest.given()
                .when("?- ar(1,2) = ar(1,2,3).")
                .assertFailed();
        PrologTest.given()
                .when("?- ar(1,2,3) = ar(1,2).")
                .assertFailed();
    }

    @Test
    public void testListSuccess() {
        PrologTest.given()
                .when("?- [1,2,3] = [1,2,3].")
                .assertSuccess();
    }

    @Test
    public void testListLengthMismatch() {
        PrologTest.given()
                .when("?- [1,2] = [1,2,3].")
                .assertFailed();
        PrologTest.given()
                .when("?- [1,2,3] = [1,2].")
                .assertFailed();
    }

    @Test
    public void testListVsStructure() {
        PrologTest.given()
                .when("?- [1,2,3] = '.'(1,'.'(2,'.'(3,[]))).")
                .assertSuccess();
        PrologTest.given()
                .when("?- [1|2] = '.'(1,2).")
                .assertSuccess();
    }

    @Test
    public void testListSplitting() {
        PrologTest.given()
                .when("?- [1,2,3] = [A|B].")
                .assertSuccess()
                .variable("A", isInteger(1))
                .variable("B", isList(isInteger(2), isInteger(3)));
        PrologTest.given()
                .when("?- [A,B,C] = `abc`.")
                .assertSuccess()
                .variable("A", isAtom("a"))
                .variable("B", isAtom("b"))
                .variable("C", isAtom("c"));
    }

    @Test
    public void testBiDirUnification() {
        PrologTest.given()
                .when("?- [X,2,3] = [1|B].")
                .assertSuccess()
                .variable("X", isInteger(1))
                .variable("B", isList(isInteger(2),isInteger(3)));
    }

    @Test
    public void testCoreferenceBinding() {
        // This is a particularly interesting one. Z must unify with b
        // X must unify with Z
        // Therefore X = Z = b
        PrologTest.given()
                .when("?- f(X, a(b, c)) = f(Z, a(Z, c)).")
                .assertSuccess()
                .variable("Z", isAtom("b"))
                .variable("X", isAtom("b"));
    }
}
