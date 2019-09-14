package org.jprolog.predicates;

import org.jprolog.test.Given;
import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

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
                        Matchers.isCompoundTerm("f",
                                Matchers.isUninstantiated(),
                                Matchers.isUninstantiated()));
        given()
                .when("?- copy_term(X,-10).")
                .assertSuccess()
                .variable("X", Matchers.isUninstantiated());
        given()
                .when("?- copy_term(f(a,X),f(X,b)).")
                .assertSuccess()
                .variable("X", Matchers.isAtom("a"));
        given()
                .when("?- copy_term(a, ok).")
                .assertFailed();
        given()
                .when("?- copy_term(f(a,X),f(X,b)), copy_term(f(a,X),f(X,b)).")
                .assertFailed();
    }
}
