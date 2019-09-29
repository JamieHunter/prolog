// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.constants;

import org.jprolog.exceptions.FutureDomainError;
import org.jprolog.exceptions.FutureEvaluationError;
import org.jprolog.exceptions.FutureInstantiationError;
import org.jprolog.exceptions.FutureTypeError;
import org.jprolog.expressions.Term;
import org.jprolog.expressions.TypeRank;
import org.jprolog.bootstrap.Interned;
import org.jprolog.io.WriteContext;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * A floating point number in Prolog. This is implemented using Java's Double. Note that
 * floats are not considered atoms but they are considered atomic.
 */
public final class PrologFloat extends AtomicBase implements PrologNumber {

    private final double value;

    /**
     * Construct from a double
     *
     * @param value Floating point value
     */
    public PrologFloat(double value) {
        this.value = value;
    }

    /**
     * Construct from a double - alternative syntax
     *
     * @param value Floating point value
     */
    public static PrologFloat from(double value) {
        return new PrologFloat(value);
    }

    /**
     * Utility to retrieve a float from a term.
     *
     * @param value Value assumed to be float
     * @return float value
     */
    public static PrologFloat from(Term value) {
        if (value.isNumber()) {
            return ((PrologNumber) value).toPrologFloat();
        } else if (!value.isInstantiated()) {
            throw new FutureInstantiationError(value);
        } else {
            throw new FutureTypeError(Interned.NUMBER_TYPE, value);
        }
    }

    /**
     * Return floating point value as a double.
     *
     * @return value
     */
    @Override
    public Double get() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologFloat notLessThanZero() {
        if (value < 0) {
            throw new FutureDomainError(Interned.NOT_LESS_THAN_ZERO_DOMAIN, this);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologFloat promote(PrologNumber other) {
        return other.toPrologFloat();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologFloat add(PrologNumber right) {
        return new PrologFloat(value + right.toPrologFloat().get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologFloat subtract(PrologNumber right) {
        return new PrologFloat(value - right.toPrologFloat().get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologFloat multiply(PrologNumber right) {
        return new PrologFloat(value * right.toPrologFloat().get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologFloat power(PrologNumber right) {
        double rightDouble = right.toPrologFloat().notLessThanZero().value;
        return new PrologFloat(Math.pow(value, rightDouble));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologAtomInterned lessThan(PrologNumber right) {
        return atomize(value < right.toPrologFloat().value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologAtomInterned greaterThan(PrologNumber right) {
        return atomize(value > right.toPrologFloat().value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologAtomInterned equalTo(PrologNumber right) {
        return atomize(value == right.toPrologFloat().value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologInteger round() {
        double v = value;
        if (v < 0) {
            v = value - 0.5;
        } else {
            v = value + 0.5;
        }
        return new PrologInteger(
                BigDecimal.valueOf(v).toBigInteger()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologInteger floor() {
        return new PrologInteger(
                BigDecimal.valueOf(Math.floor(value)).toBigInteger()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologInteger truncate() {
        return new PrologInteger(
                BigDecimal.valueOf(value).toBigInteger()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologInteger ceiling() {
        return new PrologInteger(
                BigDecimal.valueOf(Math.ceil(value)).toBigInteger()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologInteger toPrologInteger() {
        throw new FutureTypeError(Interned.INTEGER_TYPE, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologFloat toPrologFloat() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologFloat negate() {
        return new PrologFloat(-value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologFloat abs() {
        return new PrologFloat(Math.abs(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologFloat sign() {
        return new PrologFloat(Math.signum(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFloat() {
        return true;
    }


    /**
     * Floating point division
     *
     * @param right Right value
     * @return new value
     */
    public PrologFloat divide(PrologFloat right) {
        double result = value / right.value;
        if (!Double.isFinite(result)) {
            if (right.value == 0.0) {
                throw new FutureEvaluationError(Interned.ZERO_DIVISOR_EVALUATION, "Floating divide by zero");
            }
        }
        return new PrologFloat(result);
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
        return obj instanceof PrologFloat && value == ((PrologFloat) obj).value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Double.valueOf(value).hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(WriteContext context) throws IOException {
        context.beginAlphaNum();
        context.write(String.format(context.environment().getFlags().floatFormat.name(), value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int typeRank() {
        return TypeRank.FLOAT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareSameType(Term o) {
        return get().compareTo(((PrologFloat)o).get());
    }
}
