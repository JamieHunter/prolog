package org.jprolog.predicates;

import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

/**
 * Test dictionary manipulation predicates
 */
public class DictionaryTest {

    @Test
    public void testSimpleClauseSearch() {
        // Sets a baseline for the subsequent test
        PrologTest.
                given("old(X) :- age(X,Y), Y > 30 .")
                .and("age(john, 63).")
                .and("age(peter, 13).")
                .when("?- old(X).")
                .assertSuccess()
                .variable("X", Matchers.isAtom("john"));
    }

    @Test
    public void testAssert() {
        PrologTest.
                given("?- assert((old(X) :- age(X,Y), Y > 30)).")
                .and("?- assert(age(john, 63)).")
                .and("?- assert(age(peter, 13)).")
                .when("?- old(john).")
                .assertSuccess()
                .andWhen("?- old(X).")
                .assertSuccess()
                .variable("X", Matchers.isAtom("john"))
                .andWhen("?- old(peter).")
                .assertFailed();
    }

    @Test
    public void testAssertA() {
        PrologTest.
                given("?- assert(happy(peter)).")
                .and("?- asserta(happy(james)).")
                .when("?- happy(X).")
                .assertSuccess()
                .variable("X", Matchers.isAtom("james"));
    }

    @Test
    public void testAssertZ() {
        PrologTest.
                given("?- assert(happy(peter)).")
                .and("?- assertz(happy(james)).")
                .when("?- happy(X).")
                .assertSuccess()
                .variable("X", Matchers.isAtom("peter"));
    }

    @Test
    public void testAbolish() {
        PrologTest.given("a(1).")
                .and("a(2).")
                .and("b(1).")
                .and("b(2).")
                .and("?- set_prolog_flag(unknown, fail).")
                .when("?- a(1).")
                .assertSuccess()
                .andWhen("?- a(3).")
                .assertFailed()
                .andWhen("?- b(1).")
                .assertSuccess()
                .andWhen("?- abolish(a,1).")
                .assertSuccess()
                .andWhen("?- a(2).")
                .assertFailed()
                .andWhen("?- b(2).")
                .assertSuccess();
    }
}
