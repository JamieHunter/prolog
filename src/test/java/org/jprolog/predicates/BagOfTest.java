package org.jprolog.predicates;

import org.jprolog.test.Given;
import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

/**
 * Tests bagof, setof, findall
 */
public class BagOfTest {

    protected Given given() {
        return PrologTest
                // standard examples
                .given("legs(A, 6) :- insect(A).")
                .and("legs(A, 4) :- animal(A).")
                .and("legs(A, 8) :- spider(A).")
                .and("insect(bee).")
                .and("insect(ant).")
                .and("animal(horse).")
                .and("animal(cat).")
                .and("animal(dog).")
                .and("spider(tarantula).")
                // examples from https://www.cpp.edu/~jrfisher/www/prolog_tutorial/2_12.html
                .and("p(1,3,5).")
                .and("p(2,4,1).")
                .and("p(3,5,2).")
                .and("p(4,3,1).")
                .and("p(5,2,4).");
    }

    @Test
    public void testBagOfWithBacktracking() {
        given()
                .when("?- bagof(A, legs(A,N), B).")
                .assertSuccess()
                .variable("A", Matchers.isUninstantiated())
                .variable("N", Matchers.isInteger(6))
                .variable("B", Matchers.isList(Matchers.isAtom("bee"), Matchers.isAtom("ant")))
                .anotherSolution()
                .assertSuccess()
                .variable("A", Matchers.isUninstantiated())
                .variable("N", Matchers.isInteger(4))
                .variable("B", Matchers.isList(Matchers.isAtom("horse"), Matchers.isAtom("cat"), Matchers.isAtom("dog")))
                .anotherSolution()
                .assertSuccess()
                .variable("A", Matchers.isUninstantiated())
                .variable("N", Matchers.isInteger(8))
                .variable("B", Matchers.isList(Matchers.isAtom("tarantula")))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testBagOfNumberSet() {
        given()
                .when("?- bagof(Z, p(X,Y,Z), Bag).")
                .assertSuccess()
                .variable("Bag", Matchers.isList(Matchers.isInteger(5)))
                .variable("X", Matchers.isInteger(1))
                .variable("Y", Matchers.isInteger(3))
                .anotherSolution()
                .assertSuccess()
                .variable("Bag", Matchers.isList(Matchers.isInteger(1)))
                .variable("X", Matchers.isInteger(2))
                .variable("Y", Matchers.isInteger(4))
                .anotherSolution()
                .assertSuccess()
                .variable("Bag", Matchers.isList(Matchers.isInteger(2)))
                .variable("X", Matchers.isInteger(3))
                .variable("Y", Matchers.isInteger(5))
                .anotherSolution()
                .assertSuccess()
                .variable("Bag", Matchers.isList(Matchers.isInteger(1)))
                .variable("X", Matchers.isInteger(4))
                .variable("Y", Matchers.isInteger(3))
                .anotherSolution()
                .assertSuccess()
                .variable("Bag", Matchers.isList(Matchers.isInteger(4)))
                .variable("X", Matchers.isInteger(5))
                .variable("Y", Matchers.isInteger(2))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testBagOfEmpty() {
        given()
                .when("?- bagof(Z, legs(Z, 10), Bag).")
                .assertFailed();
    }

    @Test
    public void testFindAllNumberSet() {
        given()
                .when("?- findall(Z, p(X,Y,Z), Bag).")
                .assertSuccess()
                .variable("Bag", Matchers.isList(Matchers.isInteger(5), Matchers.isInteger(1), Matchers.isInteger(2), Matchers.isInteger(1), Matchers.isInteger(4)))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testFindAllEmpty() {
        given()
                .when("?- findall(Z, legs(Z, 10), Bag).")
                .assertSuccess()
                .variable("Bag", Matchers.isList());
    }

    @Test
    public void testBagOfCarotNumberSet() {
        given()
                .when("?- bagof(Z, X^Y^p(X,Y,Z), Bag).")
                .assertSuccess()
                .variable("Bag", Matchers.isList(Matchers.isInteger(5), Matchers.isInteger(1), Matchers.isInteger(2), Matchers.isInteger(1), Matchers.isInteger(4)))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testSetOfCarotNumberSet() {
        given()
                .when("?- setof(Z, X^Y^p(X,Y,Z), Bag).")
                .assertSuccess()
                .variable("Bag", Matchers.isList(Matchers.isInteger(1), Matchers.isInteger(2), Matchers.isInteger(4), Matchers.isInteger(5)))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testBagOfOverOr() {
        given()
                // from sec810.pl, resulted in unbound variable casting error
                .when("?- bagof(X, (X=Y;X=Z), S).")
                .assertSuccess()
                .variable("S", Matchers.isList(Matchers.isUninstantiated(), Matchers.isUninstantiated()))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testSetOfWithUnboundVariables() {
        given()
                // from sec810.pl, edge case fails
                .when("?- setof(X, member(X, [f(b, U), f(c, V)]), [f(b, a), f(c, a)]).")
                .assertSuccess()
                .anotherSolution()
                .assertFailed();
        given()
                // from sec810.pl, edge case fails
                .when("?- setof(X, member(X, [V, U, f(U), f(V)]), [a, b, f(a), f(b)]).")
                .assertSuccess()
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testBagOfWithUnboundInriaSuite() {
        // this is the Inria Suite Test bagof(A,(A=B;A=C),D)
        given()
                .when("?- bagof(A,(A=B;A=C),D).")
                .assertSuccess()
                .variable("D", Matchers.isList(Matchers.isVariable("B"), Matchers.isVariable("C")));
                ;
    }
}
