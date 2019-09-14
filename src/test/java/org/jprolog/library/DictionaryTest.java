package org.jprolog.library;

import org.junit.jupiter.api.Test;
import org.jprolog.test.PrologTest;

import static org.jprolog.test.Matchers.*;

/**
 * Series of tests for simple library lookup and matching
 */
public class DictionaryTest {

    @Test
    public void testSimpleFactQuery() {
        PrologTest.given("old.")
                .when("?- old.")
                .assertSuccess();
    }

    @Test
    public void testUnifyingQuery() {
        PrologTest
                .given("old(fred).")
                .when("?- old(Who).")
                .assertSuccess()
                .variable("Who", isAtom("fred"));
    }

    @Test
    public void testUnifyingQueryMultiClause() {
        PrologTest
                .given("old(fred) :- false.")
                .and("old(jim) :- true.")
                .when("?- old(Who).")
                .assertSuccess()
                .variable("Who", isAtom("jim"));
    }

    @Test
    public void testFailingQueryMultiClause() {
        PrologTest
                .given("old(fred) :- false.")
                .and("old(jim) :- true.")
                .when("?- old(jack).")
                .assertFailed();
    }

    @Test
    public void testFailingQueryMultiClauseUninstantiated() {
        PrologTest
                .given("old(fred) :- false.")
                .and("old(jim) :- false.")
                .when("?- old(X).")
                .assertFailed()
                // This below test seems odd. Variables are not meaningful on a failed query
                // Optimization in this case determines old(jim) is deterministic, and doesn't
                // add any backtracking information to undo this binding. So this behavior
                // is actually correct.
                .variable("X", isAtom("jim"));
    }

    @Test
    public void testConjunction() {
        PrologTest
                .given("thief(john).")
                .and("likes(mary, chocolate).")
                .and("likes(mary, wine).")
                .and("likes(john, X) :- likes(X, wine).")
                .and("may_steal(X, Y) :- thief(X), likes(X, Y).")
                .when("?- may_steal(john, Z).")
                .assertSuccess()
                .variable("Z", isAtom("mary"));
    }

    @Test
    public void testNontrivialBacktracking() {
        PrologTest
                .given("a(foo).")
                .and("a(bar).")
                .and("c(X) :- a(X), X=bar.")
                .when("?- c(X).")
                .variable("X", isAtom("bar"));
    }
}
