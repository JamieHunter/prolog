// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Function;
import prolog.constants.Atomic;
import prolog.constants.PrologFloat;
import prolog.constants.PrologInteger;
import prolog.constants.PrologNumber;
import prolog.functions.BinaryFunction;
import prolog.functions.UnaryFunction;

import java.math.BigInteger;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Bootstraps all the builtin evaluable terms used on right side of "is" or on left or right side of an arithmetic
 * compare operation.
 */
public final class Arithmetic {
    private Arithmetic() {
        // Static methods/fields only
    }

    /**
     * Addition.
     */
    @Function(value = "+", arity = 2)
    public static final BinaryFunction ADD = binary(PrologNumber::add);
    /**
     * Subtract.
     */
    @Function(value = "-", arity = 2)
    public static final BinaryFunction SUBTRACT = binary(PrologNumber::subtract);
    /**
     * Multiplication.
     */
    @Function(value = "*", arity = 2)
    public static final BinaryFunction MULTIPLY = binary(PrologNumber::multiply);
    /**
     * Integer division.
     */
    @Function(value = "//", arity = 2)
    public static final BinaryFunction INTEGER_DIVIDE = binaryInteger(PrologInteger::divide);
    /**
     * Integer modulo
     */
    @Function(value = "mod", arity = 2)
    public static final BinaryFunction INTEGER_MOD = binaryInteger(PrologInteger::mod);
    /**
     * Floating point divide
     */
    @Function(value = "/", arity = 2)
    public static final BinaryFunction FLOAT_DIVIDE = binaryFloat(PrologFloat::divide);
    /**
     * Negate
     */
    @Function(value = "-", arity = 1)
    public static final UnaryFunction NEGATE = unary(PrologNumber::negate);
    /**
     * Positive (no-op)
     */
    @Function(value = "+", arity = 1)
    public static final UnaryFunction POSITIVE = unary(x -> x);
    /**
     * Absolute value
     */
    @Function(value = "abs", arity = 1)
    public static final UnaryFunction ABS = unary(PrologNumber::abs);
    /**
     * Sign (number is kept as integer or float)
     */
    @Function(value = "sign", arity = 1)
    public static final UnaryFunction SIGN = unary(PrologNumber::sign);
    /**
     * Convert number to floating point
     */
    @Function(value = "float", arity = 1)
    public static final UnaryFunction FLOAT = unary(PrologNumber::toPrologFloat);
    /**
     * Convert number to integer
     */
    @Function(value = "round", arity = 1)
    public static final UnaryFunction ROUND = unary(PrologNumber::round);
    //
    // ========================================================
    //

    /**
     * Utility - construct Unary function from inline lambda function.
     *
     * @param fn Lambda function
     * @return Prolog unary function
     */
    private static UnaryFunction unary(java.util.function.Function<PrologNumber, Atomic> fn) {
        return new UnaryFunction(fn);
    }

    /**
     * Utility - construct Unary function from inline lambda function when integer value is expected.
     *
     * @param fn Lambda function
     * @return Prolog unary function
     */
    private static UnaryFunction unaryInteger(java.util.function.Function<BigInteger, Atomic> fn) {
        return unary(x -> fn.apply(x.toPrologInteger().get()));
    }

    /**
     * Utility - construct Unary function from inline lambda function when floating point value is expected.
     *
     * @param fn Lambda function
     * @return Prolog unary function
     */
    private static UnaryFunction unaryFloat(java.util.function.DoubleFunction<Atomic> fn) {
        return unary(x -> fn.apply(x.toPrologFloat().get()));
    }

    /**
     * Utility - construct Binary function from inline lambda function (implementation helper, not used directly).
     *
     * @param fn Lambda function
     * @return Prolog unary function
     */
    private static BinaryFunction binaryBase(
            java.util.function.BiFunction<PrologNumber, PrologNumber, Atomic> fn) {
        return new BinaryFunction(fn);
    }

    /**
     * Utility - construct Binary function from inline lambda function when type promotion is required.
     *
     * @param fn Lambda function
     * @return Prolog unary function
     */
    private static BinaryFunction binary(
            java.util.function.BiFunction<PrologNumber, PrologNumber, Atomic> fn) {
        return binaryBase((left, right) -> fn.apply(right.promote(left), left.promote(right)));
    }

    /**
     * Utility - construct Unary function from inline lambda function when both values must be integer.
     *
     * @param fn Lambda function
     * @return Prolog unary function
     */
    private static BinaryFunction binaryInteger(
            java.util.function.BiFunction<PrologInteger, PrologInteger, Atomic> fn) {
        return binaryBase((left, right) -> fn.apply(left.toPrologInteger(), right.toPrologInteger()));
    }

    /**
     * Utility - construct Unary function from inline lambda function when both values must be floating point.
     *
     * @param fn Lambda function
     * @return Prolog unary function
     */
    private static BinaryFunction binaryFloat(
            java.util.function.BiFunction<PrologFloat, PrologFloat, Atomic> fn) {
        return binaryBase((left, right) -> fn.apply(left.toPrologFloat(), right.toPrologFloat()));
    }

}
