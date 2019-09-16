package org.jprolog.predicates;

import org.jprolog.test.Given;
import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

/**
 * Test tail call recursion and tail call elimination
 */
public class TailCallRecursionTest {

    protected Given givenTailCall() {
        return PrologTest.given("!(1,X,X) :- '##call_depth','##backtrack_depth'.")
                .and("!(P,Q,R) :- Pnext is P-1, Qnext is P * Q , !(Pnext, Qnext, R).")
                .and("!(X,Y) :- !(X,1,Y).");
    }

    protected Given givenTailCallCall() {
        return PrologTest.given("!(1,X,X) :- '##call_depth','##backtrack_depth'.")
                .and("!(P,Q,R) :- Pnext is P-1, Qnext is P * Q , call(!(Pnext, Qnext, R)).")
                .and("!(X,Y) :- !(X,1,Y).");
    }

    @Test
    public void testTailCall() {
        // Straight forward tail-call. Eliminates most of call and backtrack stacks
        givenTailCall().when("?- !(10,X).")
                .variable("X", Matchers.isInteger(3628800))
                // call(1) = clause-end for !(10,X)
                // call(0) = terminal
                .callDepth(equalTo(2))
                // backtrack(3) = coreference variable binding
                // backtrack(2) = X binding for !(1,X,X)
                // backtrack(1) = !(1,X,X) which also matches !(P,Q,R)
                // backtrack(0) = terminal
                .backtrackDepth(equalTo(4));
    }

    @Test
    public void testTailCallCall() {
        // If the tail is wrapped in a call. Call adds no additional overhead to backtrack stacks.
        givenTailCallCall().when("?- !(10,X).")
                .variable("X", Matchers.isInteger(3628800))
                // call(1) = clause-end for !(10,X)
                // call(0) = terminal
                .callDepth(equalTo(2))
                // backtrack(3) = coreference variable binding
                // backtrack(2) = X binding for !(1,X,X)
                // backtrack(1) = !(1,X,X) which also matches !(P,Q,R)
                // backtrack(0) = terminal
                .backtrackDepth(equalTo(4));
    }
}
