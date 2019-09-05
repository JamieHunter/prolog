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
     * Guards the number throwing an error if number is less than zero.
     * @return this number
     */
    PrologNumber notLessThanZero();

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
     * Round this number to an integer
     *
     * @return round
     */
    default PrologInteger round() {
        return toPrologInteger();
    }

    /**
     * Round this number down to an integer
     *
     * @return floor
     */
    default PrologInteger floor() {
        return toPrologInteger();
    }

    /**
     * Truncate this number to an integer
     *
     * @return truncated
     */
    default PrologInteger truncate() { return toPrologInteger(); }

    /**
     * Round this number up to an integer
     *
     * @return ceiling
     */
    default PrologInteger ceiling() {
        return toPrologInteger();
    }

    /**
     * Multiply this number with the other number
     *
     * @param right Other number
     * @return product
     */
    PrologNumber multiply(PrologNumber right);

    /**
     * Raise this number to power of the other number
     *
     * @param right Other number
     * @return power
     */
    PrologNumber power(PrologNumber right);

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
    PrologAtomInterned equalTo(PrologNumber right);

    /**
     * Compare this number to another number
     *
     * @param right right constant to compare with
     * @return 'true' or 'false'
     */
    PrologAtomInterned lessThan(PrologNumber right);

    /**
     * Compare this number to another number
     *
     * @param right right constant to compare with
     * @return 'true' or 'false'
     */
    PrologAtomInterned greaterThan(PrologNumber right);
}
