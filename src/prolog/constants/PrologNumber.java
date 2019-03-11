// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.constants;

/**
 * A constant number in Prolog, Integer or Float.
 */
public interface PrologNumber extends Atomic {

    /**
     * Return a Java equivalent object
     *
     * @return number object
     */
    Number get();

    /**
     * @return true indicating this is a number
     */
    @Override
    default boolean isNumber() {
        return true;
    }

    /**
     * Promote other constant in light of this constant
     *
     * @param other constant to be promoted
     * @return promoted value or self
     */
    PrologNumber promote(PrologNumber other);

    /**
     * Add this number to the other number
     *
     * @param right Other number
     * @return sum of numbers
     */
    PrologNumber add(PrologNumber right);

    /**
     * Subtract this number from the other number
     *
     * @param right Other number
     * @return difference of numbers
     */
    PrologNumber subtract(PrologNumber right);

    /**
     * Negate this number
     *
     * @return negated
     */
    PrologNumber negate();

    /**
     * Absolute value of this number
     *
     * @return abs value
     */
    PrologNumber abs();

    /**
     * Sign of this number as an integer
     *
     * @return sign
     */
    PrologNumber sign();

    /**
     * Multiply this number with the other number
     *
     * @param right Other number
     * @return product
     */
    PrologNumber multiply(PrologNumber right);

    /**
     * Convert number to an integer
     *
     * @return Prolog integer
     */
    PrologInteger toPrologInteger();

    /**
     * Convert number to a float
     *
     * @return Prolog float
     */
    PrologFloat toPrologFloat();

    /**
     * Compare this number to another number
     *
     * @param right right constant to compare with
     * @return 'true' or 'false'
     */
    PrologAtom equalTo(PrologNumber right);

    /**
     * Compare this number to another number
     *
     * @param right right constant to compare with
     * @return 'true' or 'false'
     */
    PrologAtom lessThan(PrologNumber right);

    /**
     * Compare this number to another number
     *
     * @param right right constant to compare with
     * @return 'true' or 'false'
     */
    PrologAtom greaterThan(PrologNumber right);
}
