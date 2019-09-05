package prolog.predicates;

import org.junit.jupiter.api.Test;
import prolog.test.Given;
import prolog.test.PrologTest;

import static prolog.test.Matchers.*;

public class CopyTermTest {

    Given given() {
        return PrologTest.
                given();
    }

    @Test
    public void testBasics() {
        given()
                .when("?- copy_term(f(X,Y),Z), X=1, Y=2.")
                .assertSuccess()
                .variable("Z",
                        isCompoundTerm("f",
                                isUninstantiated(),
                                isUninstantiated()));
        given()
                .when("?- copy_term(X,-10).")
                .assertSuccess()
                .variable("X", isUninstantiated());
        given()
                .when("?- copy_term(f(a,X),f(X,b)).")
                .assertSuccess()
                .variable("X", isAtom("a"));
        given()
                .when("?- copy_term(a, ok).")
                .assertFailed();
        given()
                .when("?- copy_term(f(a,X),f(X,b)), copy_term(f(a,X),f(X,b)).")
                .assertFailed();
    }
}
