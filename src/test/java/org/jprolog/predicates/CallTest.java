package org.jprolog.predicates;

import org.hamcrest.MatcherAssert;
import org.jprolog.exceptions.PrologTypeError;
import org.jprolog.expressions.Term;
import org.jprolog.test.Given;
import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.any;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CallTest {

    protected Given given() {
        return PrologTest.given("old(X) :- age(X,Y), Y > 30 .")
                .and("age(john, 63).")
                .and("age(peter, 13).");

    }

    @Test
    public void testCallConst() {
        given().when("?- call(old(john)).")
                .assertSuccess();
        given().when("?- call(old(peter)).")
                .assertFailed();
    }

    @Test
    public void testCallIndirect() {
        given().when("?- X=old(john), call(X).")
                .assertSuccess();
        given().when("?- X=old(peter), call(X).")
                .assertFailed();
    }

    @Test
    public void testCallWithVar() {
        given().when("?- X=old(Y), call(X).")
                .assertSuccess()
                .variable("Y", Matchers.isAtom("john"));
    }

    @Test
    public void testCallableError() {
        // This verifies that call reports the entire expression in error,
        // not just the single term. This also catches an edge case that was failing.
        PrologTypeError err = assertThrows(PrologTypeError.class,
                () -> given().when("?- call((1;true))."));
        MatcherAssert.assertThat(err.value(), Matchers.isCompoundTerm("error",
                Matchers.isCompoundTerm("type_error",
                        Matchers.isAtom("callable"),
                        Matchers.isCompoundTerm(";",
                                Matchers.isInteger(1),
                                Matchers.isAtom("true"))),
                any(Term.class)));
    }
}
