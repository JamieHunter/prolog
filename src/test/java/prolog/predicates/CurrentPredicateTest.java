package prolog.predicates;

import org.junit.jupiter.api.Test;
import prolog.exceptions.PrologTypeError;
import prolog.test.Given;
import prolog.test.PrologTest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static prolog.test.Matchers.*;

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
                .variable("Arity", isInteger(1))
                .anotherSolution()
                .assertSuccess()
                .variable("Arity", isInteger(2))
                .anotherSolution()
                .assertFailed();
        given().when("?- current_predicate(b/Arity).")
                .assertSuccess()
                .variable("Arity", isInteger(2))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testByArity() {
        given().when("?- current_predicate(Functor/1).")
                .assertSuccess()
                .variable("Functor", isAtom("a"))
                .anotherSolution()
                .assertFailed();
        given().when("?- current_predicate(Functor/2).")
                .assertSuccess()
                .variable("Functor", isAtom("a"))
                .anotherSolution()
                .assertSuccess()
                .variable("Functor", isAtom("b"))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testByWild() {
        given().when("?- current_predicate(Functor/Arity).")
                .assertSuccess()
                .variable("Functor", isAtom("a"))
                .variable("Arity", isInteger(1))
                .anotherSolution()
                .assertSuccess()
                .variable("Functor", isAtom("a"))
                .variable("Arity", isInteger(2))
                .anotherSolution()
                .assertSuccess()
                .variable("Functor", isAtom("b"))
                .variable("Arity", isInteger(2))
                .anotherSolution()
                .assertFailed();
        given().when("?- current_predicate(I).")
                .assertSuccess()
                .variable("I", isCompoundTerm("/", isAtom("a"), isInteger(1)))
                .anotherSolution()
                .assertSuccess()
                .variable("I", isCompoundTerm("/", isAtom("a"), isInteger(2)))
                .anotherSolution()
                .assertSuccess()
                .variable("I", isCompoundTerm("/", isAtom("b"), isInteger(2)))
                .anotherSolution()
                .assertFailed();
    }
}
