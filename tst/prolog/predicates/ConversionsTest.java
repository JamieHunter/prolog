package prolog.predicates;

import org.junit.Test;
import prolog.test.Given;
import prolog.test.PrologTest;

import static prolog.test.Matchers.*;

/**
 * Tests various conversions
 */
public class ConversionsTest {

    protected Given given() {
        return PrologTest.given();
    }

    @Test
    public void testAtomChars() {
        given()
                .when("?- atom_chars(foo, [f,o,o]).")
                .assertSuccess()
                .andWhen("?- atom_chars(X, [b,a,r]).")
                .assertSuccess()
                .variable("X", isAtom("bar"))
                .andWhen("?- atom_chars(bx, Y).")
                .assertSuccess()
                .variable("Y", isList(isAtom("b"), isAtom("x")))
                .andWhen("?- atom_chars(Q, \"pq\").")
                .assertSuccess()
                .variable("Q", isAtom("pq"));
    }

    @Test
    public void testAtomCodes() {
        given()
                .when("?- atom_codes(X, [65, 66]).")
                .assertSuccess()
                .variable("X", isAtom("AB"))
                .andWhen("?- atom_codes(ab, Y).")
                .assertSuccess()
                .variable("Y", isList(isInteger('a'), isInteger('b')))
                ;
    }

    @Test
    public void testNumberChars() {
        given()
                .when("?- number_chars(X, ['3', '.', '3', 'E', '+', '0', '1']).")
                .assertSuccess()
                .variable("X", isFloat(3.3e+01))
                .andWhen("?- number_chars(33.0, ['3', '.', '3', 'E', '+', '0', '1']).")
                .assertSuccess()
                .andWhen("?- number_chars(33.0, Y).")
                .assertSuccess()
                .variable("Y", isList(isAtom("3"), isAtom("3"), isAtom("."), isAtom("0")))
        ;

    }

    @Test
    public void testNumberCodes() {
        given()
                .when("?- number_codes(33, [0'3, X]).")
                .assertSuccess()
                .variable("X", isInteger('3'))
        ;
    }
}
