package prolog.predicates;

import org.junit.jupiter.api.Test;
import prolog.test.Given;
import prolog.test.PrologTest;

import static prolog.test.Matchers.*;

/**
 * Test the "clause" predicate
 */
public class ClauseTest {

    protected Given given() {
        return PrologTest
                .given("a(1,2,3) :- x,y,z.")
                .and("a(1,2,3) :- x,y.")
                .and("a(1,_,4) :- x.")
                .and("a(1,2,4) :- p,q.")
                .and("a(1,2,_) :- z.")
                .and("a :- b.");
    }

    @Test
    public void testFixedHead() {
        given().when("?- clause(a(1,2,3),X).")
                .assertSuccess()
                .variable("X",
                        isCompoundTerm(",", isAtom("x"),
                                isCompoundTerm(",", isAtom("y"), isAtom("z"))));
    }

    @Test
    public void testMixed() {
        given().when("?- clause(a(A,B,C),(p,D)).")
                .assertSuccess()
                .variable("A", isInteger(1))
                .variable("B", isInteger(2))
                .variable("C", isInteger(4))
                .variable("D", isAtom("q"));
    }

    @Test
    public void testAtom() {
        given().when("?- clause(a,b).")
                .assertSuccess();
    }
}
