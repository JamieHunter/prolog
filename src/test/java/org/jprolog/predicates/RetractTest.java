package org.jprolog.predicates;

import org.jprolog.test.Given;
import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

/**
 * Test term manipulation predicates
 */
public class RetractTest {

    protected Given given() {
        return PrologTest
                .given("a(1).")
                .and("a(2).")
                .and("a(3).")
                .and("a(X) :- X=4.");
    }

    @Test
    public void testRetract() {
        given()
                .when("?- a(1).")
                .assertSuccess()
                .andWhen("?- retract(a(1)).")
                .assertSuccess()
                .andWhen("?- a(1).")
                .assertFailed();
    }

    @Test
    public void testRetractWithInstantiate() {
        given()
                .when("?- clause(a(_),_).")
                .assertSuccess()
                .andWhen("?- retract(a(X)).")
                .assertSuccess()
                .variable("X", Matchers.isInteger(1))
                .andWhen("?- retract(a(X)).")
                .assertSuccess()
                .variable("X", Matchers.isInteger(2))
                .andWhen("?- retract(a(X)).")
                .assertSuccess()
                .variable("X", Matchers.isInteger(3))
                .andWhen("?- retract(a(X)).")
                .assertFailed()
                .andWhen("?- retract(a(X) :- _).")
                .assertSuccess()
                .variable("X", Matchers.isUninstantiated())
                .andWhen("?- retract(a(X) :- _).")
                .assertFailed()
                .andWhen("?- clause(a(_),_).")
                .assertFailed();
    }

    @Test
    public void testRetractWithClause() {
        given()
                .when("?- a(4).")
                .assertSuccess()
                .andWhen("?- retract((a(Y) :- Y=4)).")
                .assertSuccess()
                .andWhen("?- a(4).")
                .assertFailed()
                .andWhen("?- retract((a(Y) :- Y=4)).")
                .assertFailed();
    }

    @Test
    public void testRetractWithClauseEdgeCase() {
        // TODO: This feels like it should be an error?
        // Verify behavior
        given()
                .when("?- a(4).")
                .assertSuccess()
                .andWhen("?- retract((a(1) :- 1=4)).") // Valid when X=1, so matches
                .assertSuccess()
                .andWhen("?- a(4).")
                .assertFailed()
                .andWhen("?- a(1).")
                .assertSuccess();
    }

    @Test
    public void testRetractSpider() {
        // from sec89.pl
        given()
                .that("legs(A, 6) :- insect(A).")
                .and("legs(A, 8) :- spider(A).")
                .and("spider(itsy).")
                .when("?- retract(legs(spider,6)).")
                .assertFailed()
                .andWhen("?- retract(legs(spider,6) :- _).")
                .assertSuccess();
    }

    @Test
    public void testRetractAll() {
        given()
                .when("?- clause(a(_),_).")
                .assertSuccess()
                .andWhen("?- retractall(a(X) :- _).")
                .assertSuccess()
                .andWhen("?- clause(a(_),_).")
                .assertFailed()
                .andWhen("?- retractall(a(X) :- _).")
                .assertSuccess();
    }
}
