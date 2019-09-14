package org.jprolog.predicates;

import org.jprolog.test.Given;
import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

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
                        Matchers.isCompoundTerm(",", Matchers.isAtom("x"),
                                Matchers.isCompoundTerm(",", Matchers.isAtom("y"), Matchers.isAtom("z"))));
    }

    @Test
    public void testMixed() {
        given().when("?- clause(a(A,B,C),(p,D)).")
                .assertSuccess()
                .variable("A", Matchers.isInteger(1))
                .variable("B", Matchers.isInteger(2))
                .variable("C", Matchers.isInteger(4))
                .variable("D", Matchers.isAtom("q"));
    }

    @Test
    public void testAtom() {
        given().when("?- clause(a,b).")
                .assertSuccess();
    }
}
