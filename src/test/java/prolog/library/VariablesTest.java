package prolog.library;

import org.junit.jupiter.api.Test;
import prolog.test.PrologTest;

import static prolog.test.Matchers.*;

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
                .variable("T", isList())
                .andWhen("?- term_variables(foo(P,Q,_,C,D),T).")
                .assertSuccess()
                .variable("T", isList(
                        isVariable("P"),
                        isVariable("Q"),
                        isUninstantiated(),
                        isVariable("C"),
                        isVariable("D")
                ))
                .andWhen("?- term_variables(P,T).")
                .assertSuccess()
                .variable("T", isList(isVariable("P")))
                .andWhen("?- term_variables(1,T).")
                .assertSuccess()
                .variable("T", isList())
                .andWhen("?- term_variables(foo(P,Q,_,C,D),[P,Q,_,C,D]).")
                .assertSuccess()
                .andWhen("?- term_variables(foo(P,Q,_,C,D),[1,2,3,4]).")
                .assertFailed()
                .andWhen("?- term_variables(foo(P,Q,_,C,D),[1,2,3,4,5]).")
                .assertSuccess()
                .variable("P", isInteger(1))
                .variable("Q", isInteger(2))
                .variable("C", isInteger(4))
                .variable("D", isInteger(5));
    }

    @Test
    void testTermVariables3() {
        PrologTest.given()
                .when("?- term_variables(foo(P,Q,_,C,D),[P,D,X,_],T).")
                .assertSuccess()
                .variable("T", isList(
                        isVariable("Q"),
                        isUninstantiated(),
                        isVariable("C")
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
                .variable("A", isCompoundTerm("$VAR", isInteger(2)))
                .variable("B", isCompoundTerm("$VAR", isInteger(3)));
    }
}
