package prolog.library;

import org.junit.Test;
import prolog.test.Given;
import prolog.test.PrologTest;

import static prolog.test.Matchers.isAtom;

public class CollationTest {

    protected Given given() {
        return PrologTest.given();
    }

    @Test
    public void testOrderLessThan() {
        given()
                .when("?- 5 @< a.") // integer vs atom
                .assertSuccess()
                .andWhen("?- 1.0 @< 5.") // float vs integer
                .assertSuccess()
                .andWhen("?- 1.0 @< 2.0.") // float vs float
                .assertSuccess()
                .andWhen("?- 10 @< 10.")
                .assertFailed();
    }

    @Test
    public void testOrderLessOrEqual() {
        given()
                .when("?- foo @=< z.") // atom vs atom
                .assertSuccess()
                .andWhen("?- foo @=< foo.") // equal
                .assertSuccess()
                .andWhen("?- foo @=< bar.") // fail
                .assertFailed();
    }

    @Test
    public void testOrderGreaterThan() {
        given()
                .when("?- foo(1,2) @> bar(1).") // arity rules over functor order
                .assertSuccess()
                .andWhen("?- foo(1) @> bar(2).") // functor if arity is the same
                .assertSuccess()
                .andWhen("?- foo(1,1) @> foo(1,0).")
                .assertSuccess()
                .andWhen("?- foo(1,1) @> foo(1,1).")
                .assertFailed()
                ;
    }

    @Test
    public void testOrderGreaterOrEqual() {
        given()
                .when("?- X=1, Y=1, X @>= Y.") // with values
                .assertSuccess()
                .andWhen("?- X=2, Y=1, X @>= Y.")
                .assertSuccess()
                .andWhen("?- X=1, Y=2, X @>= Y.")
                .assertFailed()
        ;
    }

    @Test
    public void testNotEqual() {
        given()
                .when("?- X=1, Y=2, X \\== Y.") // with values
                .assertSuccess()
                .andWhen("?- P \\== Q.") // no values
                .assertSuccess()
                .andWhen("?- A=B, B=C, C=D, A \\== D.") // no values
                .assertFailed()
        ;
    }

    @Test
    public void testEqual() {
        given()
                .when("?- P \\== Q, P=Q, P == Q.")
                .assertSuccess()
        ;
    }

    @Test
    public void testCompare() {
        given()
                .when("?- compare(Z, P, Q).") // P was numbered before Q
                .assertSuccess()
                .variable("Z", isAtom("<"))
                .andWhen("?- compare(Z, Q, P).")
                .assertSuccess()
                .variable("Z", isAtom("<")) // Q was numbered before P
                .andWhen("?- compare(Z, 2, 1).")
                .assertSuccess()
                .variable("Z", isAtom(">")) // relation
                .andWhen("?- compare(Z, Q, Q).")
                .assertSuccess()
                .variable("Z", isAtom("=")) // relation
                .andWhen("?- S=T, compare(=, S, T).")
                .assertSuccess()
        ;
    }

}
