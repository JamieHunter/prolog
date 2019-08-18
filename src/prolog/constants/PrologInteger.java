// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.constants;

import prolog.bootstrap.Interned;
import prolog.exceptions.FutureEvaluationError;
import prolog.exceptions.FutureTypeError;
import prolog.exceptions.PrologEvaluationError;
import prolog.expressions.Term;
import prolog.expressions.TypeRank;
import prolog.io.WriteContext;

import java.io.IOException;
import java.math.BigInteger;

/**
 * An integer in Prolog. This is implemented using Java's unbounded BigInteger. Integers are not considered atoms
 * but are considered atomic.
 */
public final class PrologInteger extends AtomicBase implements PrologNumber {

    private final BigInteger value;

    /**
     * Construct from BigInteger value
     *
     * @param value BigInteger value
     */
    public PrologInteger(BigInteger value) {
        this.value = value;
    }

    /**
     * Construct from long value
     *
     * @param value Long value
     */
    public PrologInteger(long value) {
        this(BigInteger.valueOf(value));
    }

    /**
     * @return Underlying interger value
     */
    @Override
    public BigInteger get() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologNumber promote(PrologNumber other) {
        return other;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return value.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologInteger toPrologInteger() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologFloat toPrologFloat() {
        return new PrologFloat(value.doubleValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologInteger add(PrologNumber right) {
        return new PrologInteger(value.add(right.toPrologInteger().get()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologNumber subtract(PrologNumber right) {
        return new PrologInteger(value.subtract(right.toPrologInteger().value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologInteger multiply(PrologNumber right) {
        return new PrologInteger(value.multiply(right.toPrologInteger().get()));
    }

    /**
     * {@inheritDoc}
     */
    public PrologInteger divide(PrologInteger right) {
        try {
            return new PrologInteger(value.divide(right.value));
        } catch (ArithmeticException ae) {
            throw new FutureEvaluationError(Interned.ZERO_DIVISOR_EVALUATION, ae);
        }
    }

    /**
     * {@inheritDoc}
     */
    public PrologInteger mod(PrologInteger right) {
        BigInteger leftVal = value;
        BigInteger rightVal = right.value;
        int cmp = rightVal.compareTo(BigInteger.ZERO);
        if (cmp == 0) {
            throw new FutureEvaluationError(Interned.ZERO_DIVISOR_EVALUATION, "Division by zero");
        }
        if (cmp < 0) {
            leftVal = leftVal.negate();
            rightVal = rightVal.negate();
        }
        return new PrologInteger(leftVal.mod(rightVal));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologInteger negate() {
        return new PrologInteger(value.negate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologInteger abs() {
        return new PrologInteger(value.abs());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologInteger sign() {
        return new PrologInteger(value.signum());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologAtomInterned lessThan(PrologNumber right) {
        return atomize(value.compareTo(right.toPrologInteger().get()) < 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologAtomInterned greaterThan(PrologNumber right) {
        return atomize(value.compareTo(right.toPrologInteger().get()) > 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologAtomInterned equalTo(PrologNumber right) {
        return atomize(value.compareTo(right.toPrologInteger().get()) == 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInteger() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        return obj instanceof PrologInteger && value.equals(((PrologInteger) obj).value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return value.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(WriteContext context) throws IOException {
        context.beginAlphaNum();
        context.write(value.toString());
    }

    /**
     * Utility to retrieve an integer from a term.
     *
     * @param value Value assumed to be integer
     * @return integer value
     */
    public static PrologInteger from(Term value) {
        if (value.isInteger()) {
            return (PrologInteger) value;
        } else {
            throw new FutureTypeError(Interned.INTEGER_TYPE, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int typeRank() {
        return TypeRank.INTEGER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareSameType(Term o) {
        return get().compareTo(((PrologInteger)o).get());
    }
}
