package prolog.predicates;

import org.junit.Test;
import prolog.test.Given;
import prolog.test.PrologTest;

import static prolog.test.Matchers.isAtom;

public class IgnoreTest {

    protected Given given() {
        return PrologTest.given("old(X) :- age(X,Y), Y > 30, assertz(isold(X)) .")
                .and("age(john, 63).")
                .and("age(sam, 50).")
                .and("age(peter, 13).")
                .and("?- set_prolog_flag(unknown, fail).");

    }

    @Test
    public void testIgnoreSuccess() {
        given().when("?- old(john).")
                .assertSuccess()
                .andWhen("?- ignore(old(john)).")
                .assertSuccess();
        given().when("?- old(peter).")
                .assertFailed(); // for reference
        given().when("?- ignore(old(peter)).")
                .assertSuccess() // different to once()
                .andWhen("?- isold(john);isold(peter);isold(sam).")
                .assertFailed(); // old never succeeded
    }

    @Test
    public void testIgnoreWithVar() {
        given().when("?- X=old(Y), ignore(X).")
                .assertSuccess()
                .variable("Y", isAtom("john"));
    }
}
