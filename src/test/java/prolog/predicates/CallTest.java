package prolog.predicates;

import org.junit.jupiter.api.Test;
import prolog.bootstrap.Interned;
import prolog.exceptions.PrologTypeError;
import prolog.expressions.Term;
import prolog.test.Given;
import prolog.test.PrologTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static prolog.test.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

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
                .variable("Y", isAtom("john"));
    }

    @Test
    public void testCallableError() {
        // This verifies that call reports the entire expression in error,
        // not just the single term. This also catches an edge case that was failing.
        PrologTypeError err = assertThrows(PrologTypeError.class,
                () -> given().when("?- call((1;true))."));
        assertThat(err.extract(), isCompoundTerm("error",
                isCompoundTerm("type_error",
                    isAtom("callable"),
                    isCompoundTerm(";",
                            isInteger(1),
                            isAtom("true"))),
                any(Term.class)));
    }
}
