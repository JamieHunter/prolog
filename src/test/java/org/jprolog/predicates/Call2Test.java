package org.jprolog.predicates;

import org.jprolog.test.Given;
import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

import static org.jprolog.test.Matchers.isInteger;

public class Call2Test {

    protected Given given() {
        return PrologTest.given("old(X) :- age(X,Y), Y > 30 .")
                .and("age(john, 63).")
                .and("age(peter, 13).");

    }

    @Test
    public void testCallConst() {
        given().when("?- call(old, john).")
                .assertSuccess();
        given().when("?- call(old, peter).")
                .assertFailed();
        // this also tests var-args construct
        given().when("?- call(age, peter, 13).")
                .assertSuccess();
        given().when("?- call(age, john, 20).")
                .assertFailed();
    }

    @Test
    public void testCallCurry() {
        given().when("?- call(age(peter), 20).")
                .assertFailed();
        given().when("?- call(age(john), 63).")
                .assertSuccess();
    }

    @Test
    public void testCallIndirect() {
        given().when("?- X=old, Y=john, call(X, Y).")
                .assertSuccess();
        given().when("?- X=old, Y=peter, call(X, Y).")
                .assertFailed();
        given().when("?- X=age, Y=peter, Z=13, call(X, Y, Z).")
                .assertSuccess();
        given().when("?- X=age, Y=john, Z=20, call(X, Y, Z).")
                .assertFailed();
        given().when("?- X=age(Y), Y=peter, Z=20, call(X, Z).")
                .assertFailed();
        given().when("?- X=age(Y), Y=john, Z=63, call(X, Z).")
                .assertSuccess();
    }

    @Test
    public void testCallWithVar() {
        given().when("?- X=old, call(X, Y).")
                .assertSuccess()
                .variable("Y", Matchers.isAtom("john"));
    }

    @Test
    public void testCallWithVarCurry() {
        given().when("?- X=age(Y), call(X, 63).")
                .assertSuccess()
                .variable("Y", Matchers.isAtom("john"));
        given().when("?- X=age(peter), call(X, Y).")
                .assertSuccess()
                .variable("Y", Matchers.isInteger(13));
    }
}
