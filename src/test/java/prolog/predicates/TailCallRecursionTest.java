package prolog.predicates;

import org.junit.jupiter.api.Test;
import prolog.test.Given;
import prolog.test.PrologTest;

import static prolog.test.Matchers.*;
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
        givenTailCall().when("?- !(5,X).")
                .variable("X", isInteger(120))
                // call(1) = clause-end for !(5,X)
                // call(0) = terminal
                .callDepth(equalTo(3))
                // backtrack(2) = X binding for !(1,X,X)
                // backtrack(1) = !(1,X,X) which also matches !(P,Q,R)
                // backtrack(0) = terminal
                .backtrackDepth(equalTo(3));
    }

    @Test
    public void testTailCallCall() {
        // If the tail is wrapped in a call. Call adds no additional overhead to backtrack stacks.
        givenTailCallCall().when("?- !(5,X).")
                .variable("X", isInteger(120))
                // call(1) = clause-end for !(5,X)
                // call(0) = terminal
                .callDepth(equalTo(3))
                // backtrack(2) = X binding for !(1,X,X)
                // backtrack(1) = !(1,X,X) which also matches !(P,Q,R)
                // backtrack(0) = terminal
                .backtrackDepth(equalTo(3));
    }
}