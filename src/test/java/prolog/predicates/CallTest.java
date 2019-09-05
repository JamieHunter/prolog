package prolog.predicates;

import org.junit.jupiter.api.Test;
import prolog.test.Given;
import prolog.test.PrologTest;

import static prolog.test.Matchers.*;

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
}
