package prolog.library;

import org.junit.Test;
import prolog.test.Given;
import prolog.test.PrologTest;

import static prolog.test.Matchers.*;

/**
 * Tests atom manipulation
 */
public class AtomTest {

    protected Given given() {
        return PrologTest.given();
    }

    @Test
    public void testAtomLength() {
        given()
                .when("?- atom_length(foo, X).")
                .assertSuccess()
                .variable("X", isInteger(3))
                .andWhen("?- atom_length('', 0).")
                .assertSuccess();
    }

    @Test
    public void testAtomConcat() {
        given()
                .when("?- atom_concat(foo, bar, X).")
                .assertSuccess()
                .variable("X", isAtom("foobar"))
                .andWhen("?- atom_concat(foo, X, foobar).")
                .assertSuccess()
                .variable("X", isAtom("bar"))
                .andWhen("?- atom_concat(fud, X, foobar).")
                .assertFailed()
                .andWhen("?- atom_concat(X, bar, foobar).")
                .assertSuccess()
                .variable("X", isAtom("foo"))
                .andWhen("?- atom_concat(X, bad, foobar).")
                .assertFailed()
                ;
    }

    @Test
    public void testAtomConcatRecurse() {
        given()
                .when("?- atom_concat(X, Y, abc).")
                .assertSuccess()
                .variable("X", isAtom(""))
                .variable("Y", isAtom("abc"))
                .anotherSolution()
                .assertSuccess()
                .variable("X", isAtom("a"))
                .variable("Y", isAtom("bc"))
                .anotherSolution()
                .assertSuccess()
                .variable("X", isAtom("ab"))
                .variable("Y", isAtom("c"))
                .anotherSolution()
                .assertSuccess()
                .variable("X", isAtom("abc"))
                .variable("Y", isAtom(""))
                .anotherSolution()
                .assertFailed();
    }
}
