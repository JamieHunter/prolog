package org.jprolog.predicates;

import org.jprolog.exceptions.PrologTypeError;
import org.jprolog.test.Given;
import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test the "current_predicate" predicate
 */
public class CurrentPredicateTest {

    protected Given given() {
        return PrologTest
                .given("a(1) :- x.")
                .and("a(1,2).")
                .and("b(1,2) :- x,y.");
    }

    @Test
    public void testSimple() {
        given().when("?- current_predicate(a/1).")
                .assertSuccess()
                .anotherSolution()
                .assertFailed();
        given().when("?- current_predicate(b/2).")
                .assertSuccess()
                .anotherSolution()
                .assertFailed();
        given().when("?- current_predicate(current_predicate/1).")
                .assertFailed();
    }

    @Test
    public void testBadTermAtomic() {
        assertThrows(PrologTypeError.class, () -> {
            given().when("?- current_predicate(4).");
        });
    }

    @Test
    public void testBadTermEdge() {
        assertThrows(PrologTypeError.class, () -> {
            given().when("?- current_predicate(4/foo).");
        });
    }

    @Test
    public void testByName() {
        given().when("?- current_predicate(a/Arity).")
                .assertSuccess()
                .variable("Arity", Matchers.isInteger(1))
                .anotherSolution()
                .assertSuccess()
                .variable("Arity", Matchers.isInteger(2))
                .anotherSolution()
                .assertFailed();
        given().when("?- current_predicate(b/Arity).")
                .assertSuccess()
                .variable("Arity", Matchers.isInteger(2))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testByArity() {
        given().when("?- current_predicate(Functor/1).")
                .assertSuccess()
                .variable("Functor", Matchers.isAtom("a"))
                .anotherSolution()
                .assertFailed();
        given().when("?- current_predicate(Functor/2).")
                .assertSuccess()
                .variable("Functor", Matchers.isAtom("a"))
                .anotherSolution()
                .assertSuccess()
                .variable("Functor", Matchers.isAtom("b"))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testByWild() {
        given().when("?- current_predicate(Functor/Arity).")
                .assertSuccess()
                .variable("Functor", Matchers.isAtom("a"))
                .variable("Arity", Matchers.isInteger(1))
                .anotherSolution()
                .assertSuccess()
                .variable("Functor", Matchers.isAtom("a"))
                .variable("Arity", Matchers.isInteger(2))
                .anotherSolution()
                .assertSuccess()
                .variable("Functor", Matchers.isAtom("b"))
                .variable("Arity", Matchers.isInteger(2))
                .anotherSolution()
                .assertFailed();
        given().when("?- current_predicate(I).")
                .assertSuccess()
                .variable("I", Matchers.isCompoundTerm("/", Matchers.isAtom("a"), Matchers.isInteger(1)))
                .anotherSolution()
                .assertSuccess()
                .variable("I", Matchers.isCompoundTerm("/", Matchers.isAtom("a"), Matchers.isInteger(2)))
                .anotherSolution()
                .assertSuccess()
                .variable("I", Matchers.isCompoundTerm("/", Matchers.isAtom("b"), Matchers.isInteger(2)))
                .anotherSolution()
                .assertFailed();
    }
}
