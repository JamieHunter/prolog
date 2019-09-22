package org.jprolog.execution;

import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

import static org.jprolog.test.Matchers.isInteger;

public class BacktrackingTest {
    @Test
    public void testSimpleChoicePoint() {
        PrologTest.given().when("?- X=1, false; Y=1; Z=1.")
                .assertSuccess()
                .variable("X", Matchers.isUninstantiated())
                .variable("Y", isInteger(1))
                .variable("Z", Matchers.isUninstantiated());
    }

    @Test
    public void clauseSimpleChoicePoint() {
        PrologTest.given("a(1) :- false.")
                .and("a(2) :- true.")
                .and("a(3) :- false.")
                .when("?- a(X).")
                .assertSuccess()
                .variable("X", isInteger(2));
    }

    @Test
    public void clauseNestedChoicePoint() {
        PrologTest.given("a(1).")
                .and("a(2).")
                .and("a(3).")
                .and("b(N) :- a(N), N >= 2.")
                .when("?- b(X).")
                .assertSuccess()
                .variable("X", isInteger(2));
    }

    @Test
    public void clauseDoubleNestedChoicePointTail() {
        PrologTest.given("a(1).")
                .and("a(2).")
                .and("a(3).")
                .and("a(4).")
                .and("b(N) :- a(N), N >= 2.")
                .and("c(M) :- b(M), M >= 3.")
                .when("?- c(X).")
                .solutions(
                        soln -> soln.variable("X", isInteger(3)),
                        soln -> soln.variable("X", isInteger(4))
                        );
    }

    @Test
    public void clauseTailChoicePoint() {
        PrologTest.given("a(1).")
                .and("a(2).")
                .and("a(3).")
                .and("b(1, M) :- a(M).")
                .and("b(N, M) :- NN is N-1, a(N), b(NN,M).")
                .when("?- b(3, 2).")
                .assertSuccess();
    }

    @Test
    public void testAssignAtTailTopLevel() {
        PrologTest.given("a(1).").and("a(2).").and("a(3).")
                .when("?- a(N), N=P.") // N=P is in tail, and a simple instruction
                .solutions(soln -> soln.variable("P", isInteger(1)),
                        soln -> soln.variable("P", isInteger(2)),
                        soln -> soln.variable("P", isInteger(3)));
    }

    @Test
    public void testAssignAtTailNestedLevel() {
        PrologTest.given("a(1).").and("a(2).").and("a(3).").and("b(P) :- a(N), N=P.")
                .when("?- b(Q).") // N=P is in tail, and a simple instruction
                .solutions(soln -> soln.variable("Q", isInteger(1)),
                        soln -> soln.variable("Q", isInteger(2)),
                        soln -> soln.variable("Q", isInteger(3)));
    }

    @Test
    public void testAssignAtTailDoubleNestedLevel() {
        PrologTest.given("a(1).").and("a(2).").and("a(3).").and("b(P) :- a(N), N=P.").and("c(Q) :- b(Q).")
                .when("?- c(R).") // N=P is in tail, and a simple instruction
                .solutions(soln -> soln.variable("R", isInteger(1)),
                        soln -> soln.variable("R", isInteger(2)),
                        soln -> soln.variable("R", isInteger(3)));
    }
}
