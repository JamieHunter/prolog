// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.constants;

import prolog.bootstrap.Interned;
import prolog.exceptions.FutureEvaluationError;
import prolog.expressions.Term;
import prolog.expressions.TypeRank;
import prolog.io.WriteContext;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

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
    public PrologAtom lessThan(PrologNumber right) {
        return atomize(value < right.toPrologFloat().value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologAtom greaterThan(PrologNumber right) {
        return atomize(value > right.toPrologFloat().value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologAtom equalTo(PrologNumber right) {
        return atomize(value == right.toPrologFloat().value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologInteger round() {
        return new PrologInteger(
                BigDecimal.valueOf(value+0.5).toBigInteger()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologInteger toPrologInteger() {
        return new PrologInteger(
                BigDecimal.valueOf(value).toBigInteger()
        );
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
        context.write(String.valueOf(value));
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
