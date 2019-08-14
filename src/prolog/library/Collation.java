// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Interned;
import prolog.bootstrap.Predicate;
import prolog.constants.PrologAtomInterned;
import prolog.constants.PrologAtomLike;
import prolog.constants.PrologChars;
import prolog.constants.PrologCodePoints;
import prolog.constants.PrologEOF;
import prolog.exceptions.PrologInstantiationError;
import prolog.exceptions.PrologSyntaxError;
import prolog.exceptions.PrologTypeError;
import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.expressions.TermList;
import prolog.flags.ReadOptions;
import prolog.flags.WriteOptions;
import prolog.io.InputBuffered;
import prolog.io.InputDecoderFilter;
import prolog.io.OutputEncoderFilter;
import prolog.io.PrologInputStream;
import prolog.io.PrologOutputStream;
import prolog.io.SequentialInputStream;
import prolog.io.SequentialOutputStream;
import prolog.io.WriteContext;
import prolog.parser.Tokenizer;
import prolog.unification.Unifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class Collation {

    /**
     * Compare two terms for equivalence
     *
     * @param environment Execution environment
     * @param left Left term
     * @param right right term
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
     * @param left Left term
     * @param right right term
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
     * @param left Left term
     * @param right right term
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
     * @param left Left term
     * @param right right term
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
     * @param left Left term
     * @param right right term
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
     * @param left Left term
     * @param right right term
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
     * @param rel Relation
     * @param left Left term
     * @param right right term
     */
    @Predicate("compare")
    public static void orderCompare(Environment environment, Term rel, Term left, Term right) {
        int order = left.compareTo(right);
        Term res;
        if (order < 0) {
            res = Interned.LESS_THAN_ATOM;
        } else if (order > 0) {
            res = Interned.GREATER_THAN_ATOM;
        } else {
            res = Interned.EQUALS_ATOM;
        }
        if (!Unifier.unify(environment.getLocalContext(), rel, res)) {
            environment.backtrack();
        }
    }
}
