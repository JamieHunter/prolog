package prolog.predicates;

import org.junit.Test;
import prolog.test.Given;
import prolog.test.PrologTest;

import static prolog.test.Matchers.isAtom;
import static prolog.test.Matchers.isInteger;
import static prolog.test.Matchers.isUninstantiated;

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
                .variable("X", isInteger(1))
                .andWhen("?- retract(a(X)).")
                .assertSuccess()
                .variable("X", isInteger(2))
                .andWhen("?- retract(a(X)).")
                .assertSuccess()
                .variable("X", isInteger(3))
                .andWhen("?- retract(a(X)).")
                .assertSuccess()
                .variable("X", isUninstantiated())
                .andWhen("?- retract(a(X)).")
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
    public void testRetractAll() {
        given()
                .when("?- clause(a(_),_).")
                .assertSuccess()
                .andWhen("?- retractall(a(X)).")
                .assertSuccess()
                .andWhen("?- clause(a(_),_).")
                .assertFailed();
    }
}
