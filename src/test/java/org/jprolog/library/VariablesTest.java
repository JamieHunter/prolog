package org.jprolog.library;

import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

public class VariablesTest {
    @Test
    void testGround() {
        PrologTest.given().when("?- ground(foo(a,b,c)).")
                .assertSuccess()
                .andWhen("?- ground(foo(a,B,c)).")
                .assertFailed()
                .andWhen("?- ground(X).")
                .assertFailed();
    }

    @Test
    void testTermVariables2() {
        PrologTest.given().when("?- term_variables(foo(a,b,c),T).")
                .assertSuccess()
                .variable("T", Matchers.isList())
                .andWhen("?- term_variables(foo(P,Q,_,C,D),T).")
                .assertSuccess()
                .variable("T", Matchers.isList(
                        Matchers.isVariable("P"),
                        Matchers.isVariable("Q"),
                        Matchers.isUninstantiated(),
                        Matchers.isVariable("C"),
                        Matchers.isVariable("D")
                ))
                .andWhen("?- term_variables(P,T).")
                .assertSuccess()
                .variable("T", Matchers.isList(Matchers.isVariable("P")))
                .andWhen("?- term_variables(1,T).")
                .assertSuccess()
                .variable("T", Matchers.isList())
                .andWhen("?- term_variables(foo(P,Q,_,C,D),[P,Q,_,C,D]).")
                .assertSuccess()
                .andWhen("?- term_variables(foo(P,Q,_,C,D),[1,2,3,4]).")
                .assertFailed()
                .andWhen("?- term_variables(foo(P,Q,_,C,D),[1,2,3,4,5]).")
                .assertSuccess()
                .variable("P", Matchers.isInteger(1))
                .variable("Q", Matchers.isInteger(2))
                .variable("C", Matchers.isInteger(4))
                .variable("D", Matchers.isInteger(5));
    }

    @Test
    void testTermVariables3() {
        PrologTest.given()
                .when("?- term_variables(foo(P,Q,_,C,D),[P,D,X,_],T).")
                .assertSuccess()
                .variable("T", Matchers.isList(
                        Matchers.isVariable("Q"),
                        Matchers.isUninstantiated(),
                        Matchers.isVariable("C")
                ));
    }

    @Test
    void testTermNonground() {
        PrologTest.given()
                .when("?- nonground(foo(a,b),a).")
                .assertFailed()
                .andWhen("?- nonground(foo(A,B),A).")
                .assertSuccess()
                .andWhen("?- nonground(foo(A,B),B).")
                .assertSuccess()
                .andWhen("?- nonground(foo(A,B),C).")
                .assertFailed();
    }

    @Test
    void testNumberVars() {
        PrologTest.given()
                .when("?- numbervars(foo(A,B), 2, 4).")
                .assertSuccess()
                .variable("A", Matchers.isCompoundTerm("$VAR", Matchers.isInteger(2)))
                .variable("B", Matchers.isCompoundTerm("$VAR", Matchers.isInteger(3)));
    }
}
