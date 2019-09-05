package prolog.predicates;

import org.junit.Test;
import prolog.test.Given;
import prolog.test.PrologTest;

import static prolog.test.Matchers.isAtom;

public class OnceTest {

    protected Given given() {
        return PrologTest.given("old(X) :- age(X,Y), Y > 30, assertz(isold(X)) .")
                .and("age(john, 63).")
                .and("age(sam, 50).")
                .and("age(peter, 13).")
                .and("?- set_prolog_flag(unknown, fail).");

    }

    @Test
    public void testOnceSuccess() {
        given().when("?- once(old(john)).")
                .assertSuccess()
                .andWhen("?- isold(john).")
                .assertSuccess();
        given().when("?- once(old(peter)).")
                .assertFailed()
                .andWhen("?- isold(john);isold(peter);isold(sam).")
                .assertFailed();
    }

    @Test
    public void testBacktrackReference() {
        // reference, behavior expected without once
        given().when("?- old(X), false.")
                .assertFailed()
                .andWhen("?- isold(john).")
                .assertSuccess()
                .andWhen("?- isold(sam).")
                .assertSuccess();
    }

    @Test
    public void testOnceRetry() {
        // test, behavior expected with once
        given().when("?- once(old(X)), false.")
                .assertFailed()
                .andWhen("?- isold(john).")
                .assertSuccess()
                .andWhen("?- isold(sam).")
                .assertFailed();
    }

    @Test
    public void testOnceWithVar() {
        given().when("?- X=old(Y), once(X).")
                .assertSuccess()
                .variable("Y", isAtom("john"));
    }
}
