// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Compare;
import org.jprolog.bootstrap.Interned;
import org.jprolog.constants.Atomic;
import org.jprolog.constants.PrologNumber;
import org.jprolog.functions.BinaryFunction;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Bootstraps all the builtin compare operations. Compares are stack-based.
 */
public final class CompareImpl {
    private CompareImpl() {
        // Static methods/fields only
    }

    /**
     * Arithmetic.
     * Compare for equality.
     */
    @Compare("=:=")
    public static final BinaryFunction EQUAL_TO = compare(PrologNumber::equalTo);
    /**
     * Arithmetic.
     * Compare for inequality.
     */
    @Compare("=\\=")
    public static final BinaryFunction NOT_EQUAL_TO = notCompare(PrologNumber::equalTo);
    /**
     * Arithmetic.
     * Compare for less than.
     */
    @Compare("<")
    public static final BinaryFunction LESS_THAN = compare(PrologNumber::lessThan);
    /**
     * Arithmetic.
     * Compare for greater than or equal to (not less than).
     */
    @Compare(">=")
    public static final BinaryFunction NOT_LESS_THAN = notCompare(PrologNumber::lessThan);
    /**
     * Arithmetic.
     * Compare for greater than.
     */
    @Compare(">")
    public static final BinaryFunction GREATER_THAN = compare(PrologNumber::greaterThan);
    /**
     * Arithmetic.
     * Compare for less than or equal to (not greater than).
     */
    @Compare("=<")
    public static final BinaryFunction NOT_GREATER_THAN = notCompare(PrologNumber::greaterThan);

    //
    // ======================================================================
    //

    /**
     * Utility, invert true atom to false atom.
     * @param value Atomic value returned
     * @return Inverted value
     */
    private static Atomic not(Atomic value) {
        return value == Interned.TRUE_ATOM ? Interned.FALSE_ATOM : Interned.TRUE_ATOM;
    }

    /**
     * Utilily - create binary compare function from lambda.
     * @param fn Lambda function, takes two numbers, returns Atomic.
     * @return Constructed binary function.
     */
    private static BinaryFunction compare(
            java.util.function.BiFunction<PrologNumber, PrologNumber, Atomic> fn) {
        return new BinaryFunction((left, right) -> fn.apply(right.promote(left), left.promote(right)));
    }

    /**
     * Utility, create in inverted compare.
     * @param fn Lambda function, takes two numbers, returns Atomic.
     * @return Constructed binary function.
     */
    private static BinaryFunction notCompare(
            java.util.function.BiFunction<PrologNumber, PrologNumber, Atomic> fn) {
        return new BinaryFunction((left, right) -> not(fn.apply(right.promote(left), left.promote(right))));
    }
}
