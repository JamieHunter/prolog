package org.jprolog.library;

import org.junit.jupiter.api.Test;
import org.jprolog.test.Given;
import org.jprolog.test.PrologTest;

import static org.jprolog.test.Matchers.*;

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

    @Test
    public void testSubAtomFullyConstrained() {
        // examples of sub_atom where everything should be fully constrained
        given()
                .when("?- sub_atom(food, 0, 4, 0, food).")
                .assertSuccess()
                .anotherSolution()
                .assertFailed()
                .andWhen("?- sub_atom(fingerfood, 1, 5, 4, inger).")
                .assertSuccess()
                .anotherSolution()
                .assertFailed()
                .andWhen("?- sub_atom(fingerfood, 1, 5, 4, X).")
                .assertSuccess()
                .variable("X", isAtom("inger"))
                .anotherSolution()
                .assertFailed()
                .andWhen("?- sub_atom(fingerfood, 1, Y, 4, X).")
                .assertSuccess()
                .variable("X", isAtom("inger"))
                .variable("Y", isInteger(5))
                .anotherSolution()
                .assertFailed()
                .andWhen("?- sub_atom(fingerfood, B, 5, 4, X).")
                .assertSuccess()
                .variable("X", isAtom("inger"))
                .variable("B", isInteger(1))
                .anotherSolution()
                .assertFailed()
                .andWhen("?- sub_atom(fingerfood, 1, 5, A, X).")
                .assertSuccess()
                .variable("X", isAtom("inger"))
                .variable("A", isInteger(4))
                .anotherSolution()
                .assertFailed()
                .andWhen("?- sub_atom(fingerfood, 1, Y, A, inger).")
                .assertSuccess()
                .variable("Y", isInteger(5))
                .variable("A", isInteger(4))
                .anotherSolution()
                .assertFailed()
                ;
    }

    @Test
    public void testSubAtomScan() {
        // examples of sub_atom that scans for hits
        given()
                .when("?- sub_atom(foodlydooofly, B, L, A, oo).")
                .assertSuccess()
                .variable("B", isInteger(1))
                .variable("L", isInteger(2))
                .variable("A", isInteger(10))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(7))
                .variable("L", isInteger(2))
                .variable("A", isInteger(4))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(8))
                .variable("L", isInteger(2))
                .variable("A", isInteger(3))
                .anotherSolution()
                .assertFailed();

        given()
                .when("?- sub_atom(fifififi, B, L, A, fifi).")
                .assertSuccess()
                .variable("B", isInteger(0))
                .variable("L", isInteger(4))
                .variable("A", isInteger(4))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(2))
                .variable("L", isInteger(4))
                .variable("A", isInteger(2))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(4))
                .variable("L", isInteger(4))
                .variable("A", isInteger(0))
                .anotherSolution()
                .assertFailed();

        given()
                .when("?- sub_atom(fifif, B, L, A, f).")
                .assertSuccess()
                .variable("B", isInteger(0))
                .variable("L", isInteger(1))
                .variable("A", isInteger(4))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(2))
                .variable("L", isInteger(1))
                .variable("A", isInteger(2))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(4))
                .variable("L", isInteger(1))
                .variable("A", isInteger(0))
                .anotherSolution()
                .assertFailed();

        given()
                .when("?- sub_atom(fif, B, L, A, '').")
                .assertSuccess()
                .variable("B", isInteger(0))
                .variable("L", isInteger(0))
                .variable("A", isInteger(3))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(1))
                .variable("L", isInteger(0))
                .variable("A", isInteger(2))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(2))
                .variable("L", isInteger(0))
                .variable("A", isInteger(1))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(3))
                .variable("L", isInteger(0))
                .variable("A", isInteger(0))
                .anotherSolution()
                .assertFailed();

        given()
                .when("?- sub_atom(fif, B, 0, A, X).")
                .assertSuccess()
                .variable("B", isInteger(0))
                .variable("X", isAtom(""))
                .variable("A", isInteger(3))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(1))
                .variable("X", isAtom(""))
                .variable("A", isInteger(2))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(2))
                .variable("X", isAtom(""))
                .variable("A", isInteger(1))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(3))
                .variable("X", isAtom(""))
                .variable("A", isInteger(0))
                .anotherSolution()
                .assertFailed();

    }

    @Test
    public void testSubAtomFixedLen() {
        // examples of sub_atom that iterates with fixed length
        given()
                .when("?- sub_atom(abcde, B, 2, A, X).")
                .assertSuccess()
                .variable("B", isInteger(0))
                .variable("A", isInteger(3))
                .variable("X", isAtom("ab"))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(1))
                .variable("A", isInteger(2))
                .variable("X", isAtom("bc"))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(2))
                .variable("A", isInteger(1))
                .variable("X", isAtom("cd"))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(3))
                .variable("A", isInteger(0))
                .variable("X", isAtom("de"))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testSubAtomFixedLeft() {
        // examples of sub_atom that iterates with fixed beginning
        given()
                .when("?- sub_atom(abcde, 2, L, A, X).")
                .assertSuccess()
                .variable("A", isInteger(3))
                .variable("L", isInteger(0))
                .variable("X", isAtom(""))
                .anotherSolution()
                .assertSuccess()
                .variable("A", isInteger(2))
                .variable("L", isInteger(1))
                .variable("X", isAtom("c"))
                .anotherSolution()
                .assertSuccess()
                .variable("A", isInteger(1))
                .variable("L", isInteger(2))
                .variable("X", isAtom("cd"))
                .anotherSolution()
                .assertSuccess()
                .variable("A", isInteger(0))
                .variable("L", isInteger(3))
                .variable("X", isAtom("cde"))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testSubAtomFixedRight() {
        // examples of sub_atom that iterates with fixed end
        given()
                .when("?- sub_atom(abcde, B, L, 2, X).")
                .assertSuccess()
                .variable("B", isInteger(0))
                .variable("L", isInteger(3))
                .variable("X", isAtom("abc"))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(1))
                .variable("L", isInteger(2))
                .variable("X", isAtom("bc"))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(2))
                .variable("L", isInteger(1))
                .variable("X", isAtom("c"))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(3))
                .variable("L", isInteger(0))
                .variable("X", isAtom(""))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testSubAtomUnconstrained() {
        // examples of sub_atom that iterates all solutuions
        given()
                .when("?- sub_atom(abc, B, L, A, X).")
                .assertSuccess()
                .variable("B", isInteger(0))
                .variable("L", isInteger(0))
                .variable("A", isInteger(3))
                .variable("X", isAtom(""))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(0))
                .variable("L", isInteger(1))
                .variable("A", isInteger(2))
                .variable("X", isAtom("a"))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(0))
                .variable("L", isInteger(2))
                .variable("A", isInteger(1))
                .variable("X", isAtom("ab"))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(0))
                .variable("L", isInteger(3))
                .variable("A", isInteger(0))
                .variable("X", isAtom("abc"))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(1))
                .variable("L", isInteger(0))
                .variable("A", isInteger(2))
                .variable("X", isAtom(""))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(1))
                .variable("L", isInteger(1))
                .variable("A", isInteger(1))
                .variable("X", isAtom("b"))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(1))
                .variable("L", isInteger(2))
                .variable("A", isInteger(0))
                .variable("X", isAtom("bc"))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(2))
                .variable("L", isInteger(0))
                .variable("A", isInteger(1))
                .variable("X", isAtom(""))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(2))
                .variable("L", isInteger(1))
                .variable("A", isInteger(0))
                .variable("X", isAtom("c"))
                .anotherSolution()
                .assertSuccess()
                .variable("B", isInteger(3))
                .variable("L", isInteger(0))
                .variable("A", isInteger(0))
                .variable("X", isAtom(""))
                .anotherSolution()
                .assertFailed();
    }
}
