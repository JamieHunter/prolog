package prolog.predicates;

import org.junit.Test;
import prolog.test.Given;
import prolog.test.PrologTest;

import static prolog.test.Matchers.isAtom;
import static prolog.test.Matchers.isInteger;

public class CallApplyTest {

    protected Given given() {
        return PrologTest.given("old(X) :- age(X,Y), Y > 30 .")
                .and("age(john, 63).")
                .and("age(peter, 13).");

    }

    @Test
    public void testApplyConst() {
        given().when("?- apply(old, [john]).")
                .assertSuccess();
        given().when("?- apply(old, [peter]).")
                .assertFailed();
        given().when("?- apply(age, [peter, 13]).")
                .assertSuccess();
        given().when("?- apply(age, [john, 20]).")
                .assertFailed();
    }

    @Test
    public void testApplyCurry() {
        given().when("?- apply(age(peter), [20]).")
                .assertFailed();
        given().when("?- apply(age(john), [63]).")
                .assertSuccess();
    }

    @Test
    public void testCallIndirect() {
        given().when("?- X=old, Y=john, apply(X, [Y]).")
                .assertSuccess();
        given().when("?- X=old, Y=[peter], apply(X, Y).")
                .assertFailed();
        given().when("?- X=age, Y=peter, Z=13, apply(X, [Y, Z]).")
                .assertSuccess();
        given().when("?- X=age, Y=[john, 20], apply(X, Y).")
                .assertFailed();
        given().when("?- X=age(Y), Y=peter, Z=20, apply(X, [Z]).")
                .assertFailed();
        given().when("?- X=age(Y), Y=john, Z=[63], apply(X, Z).")
                .assertSuccess();
    }

    @Test
    public void testApplyWithVar() {
        given().when("?- X=old, apply(X, [Y]).")
                .assertSuccess()
                .variable("Y", isAtom("john"));
    }

    @Test
    public void testApplyWithVarCurry() {
        given().when("?- X=age(Y), apply(X, [63]).")
                .assertSuccess()
                .variable("Y", isAtom("john"));
        given().when("?- X=age(peter), apply(X, [Y]).")
                .assertSuccess()
                .variable("Y", isInteger(13));
    }
}
