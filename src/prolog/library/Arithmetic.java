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
     * To-power-of.
     */
    @Function(value = "**", arity = 2)
    public static final BinaryFunction TO_POWER_OF = binaryInteger(PrologNumber::power);
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
     * Convert number to integer, by rounding down
     */
    @Function(value = "floor", arity = 1)
    public static final UnaryFunction FLOOR = unary(PrologNumber::floor);
    /**
     * Convert number to integer, by rounding up
     */
    @Function(value = "ceiling", arity = 1)
    public static final UnaryFunction CEILING = unary(PrologNumber::ceiling);
    /**
     * Convert number to integer by rounding to nearest
     */
    @Function(value = "round", arity = 1)
    public static final UnaryFunction ROUND = unary(PrologNumber::round);
    /**
     * Convert number to integer via truncation
     */
    @Function(value = "truncate", arity = 1)
    public static final UnaryFunction TRUNCATE = unary(PrologNumber::toPrologInteger);

    @Function(value = "sin", arity = 1)
    public static final UnaryFunction SIN = unaryFloat(x -> PrologFloat.from(Math.sin(x)));

    @Function(value = "cos", arity = 1)
    public static final UnaryFunction COS = unaryFloat(x -> PrologFloat.from(Math.cos(x)));

    @Function(value = "atan", arity = 1)
    public static final UnaryFunction ATAN = unaryFloat(x -> PrologFloat.from(Math.atan(x)));

    @Function(value = "log", arity = 1)
    public static final UnaryFunction LOG = unaryFloat(x -> PrologFloat.from(Math.log(x)));

    @Function(value = "exp", arity = 1)
    public static final UnaryFunction EXP = unaryFloat(x -> PrologFloat.from(Math.exp(x)));

    @Function(value = "/\\", arity = 2)
    public static final BinaryFunction BIT_AND = binaryInteger((x,y) -> PrologInteger.from(x.get().and(y.get())));

    @Function(value = "\\/", arity = 2)
    public static final BinaryFunction BIT_OR = binaryInteger((x,y) -> PrologInteger.from(x.get().or(y.get())));

    @Function(value = "<<", arity = 2)
    public static final BinaryFunction BIT_SHIFT_LEFT = binaryInteger((x,y) -> PrologInteger.from(x.get().shiftLeft(y.toInteger())));

    @Function(value = ">>", arity = 2)
    public static final BinaryFunction BIT_SHIFT_RIGHT = binaryInteger((x,y) -> PrologInteger.from(x.get().shiftRight(y.toInteger())));

    @Function(value = "\\", arity = 1)
    public static final UnaryFunction BIT_INVERT = unaryInteger(x -> PrologInteger.from(x.not()));

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
