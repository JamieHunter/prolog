// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Interned;
import org.jprolog.bootstrap.Predicate;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.Term;
import org.jprolog.unification.Unifier;

public class Collation {

    /**
     * Compare two terms for equivalence
     *
     * @param environment Execution environment
     * @param left        Left term
     * @param right       right term
     */
    @Predicate("==")
    public static void equivalent(Environment environment, Term left, Term right) {
        if (left.compareTo(right) != 0) {
            environment.backtrack();
        }
    }

    /**
     * Compare two terms for equivalence
     *
     * @param environment Execution environment
     * @param left        Left term
     * @param right       right term
     */
    @Predicate("\\==")
    public static void notEquivalent(Environment environment, Term left, Term right) {
        if (left.compareTo(right) == 0) {
            environment.backtrack();
        }
    }

    /**
     * Compare two terms for order
     *
     * @param environment Execution environment
     * @param left        Left term
     * @param right       right term
     */
    @Predicate("@<")
    public static void orderLess(Environment environment, Term left, Term right) {
        if (left.compareTo(right) >= 0) {
            environment.backtrack();
        }
    }

    /**
     * Compare two terms for order
     *
     * @param environment Execution environment
     * @param left        Left term
     * @param right       right term
     */
    @Predicate("@=<")
    public static void orderLessEqual(Environment environment, Term left, Term right) {
        if (left.compareTo(right) > 0) {
            environment.backtrack();
        }
    }

    /**
     * Compare two terms for order
     *
     * @param environment Execution environment
     * @param left        Left term
     * @param right       right term
     */
    @Predicate("@>")
    public static void orderGreater(Environment environment, Term left, Term right) {
        if (left.compareTo(right) <= 0) {
            environment.backtrack();
        }
    }

    /**
     * Compare two terms for order
     *
     * @param environment Execution environment
     * @param left        Left term
     * @param right       right term
     */
    @Predicate("@>=")
    public static void orderGreaterEqual(Environment environment, Term left, Term right) {
        if (left.compareTo(right) < 0) {
            environment.backtrack();
        }
    }

    /**
     * Compare two terms for order
     *
     * @param environment Execution environment
     * @param rel         Relation
     * @param left        Left term
     * @param right       right term
     */
    @Predicate("compare")
    public static void orderCompare(Environment environment, Term rel, Term left, Term right) {
        int order = left.compareTo(right);
        PrologAtomLike res;
        if (order < 0) {
            res = Interned.LESS_THAN_ATOM;
        } else if (order > 0) {
            res = Interned.GREATER_THAN_ATOM;
        } else {
            res = Interned.EQUALS_ATOM;
        }
        Unifier.unifyAtom(environment, rel, res);
    }
}
