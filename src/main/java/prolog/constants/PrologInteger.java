// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.constants;

import prolog.bootstrap.Interned;
import prolog.exceptions.FutureDomainError;
import prolog.exceptions.FutureEvaluationError;
import prolog.exceptions.FutureInstantiationError;
import prolog.exceptions.FutureRepresentationError;
import prolog.exceptions.FutureTypeError;
import prolog.execution.Environment;
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
     * Construct from BigInteger - alternative syntax
     *
     * @param value Integer value
     */
    public static PrologInteger from(BigInteger value) {
        return new PrologInteger(value);
    }

    /**
     * Construct from long - alternative syntax
     *
     * @param value Integer value
     */
    public static PrologInteger from(long value) {
        return new PrologInteger(value);
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
     * Bounded long
     *
     * @return long value
     */
    public long toLong() {
        if (value.compareTo(BigInteger.ZERO) > 0) {
            if (value.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                throw new FutureDomainError(new PrologAtom("max_long"), this);
            }
        } else {
            if (value.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0) {
                throw new FutureDomainError(new PrologAtom("min_long"), this);
            }
        }
        return value.longValue();
    }

    /**
     * Bounded integer
     *
     * @return integer value
     */
    public int toInteger() {
        if (value.compareTo(BigInteger.ZERO) > 0) {
            if (value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new FutureDomainError(new PrologAtom("max_integer"), this);
            }
        } else {
            if (value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {
                throw new FutureDomainError(new PrologAtom("min_integer"), this);
            }
        }
        return value.intValue();
    }

    /**
     * Bounded character
     *
     * @return character value
     */
    public char toChar() {
        if (value.compareTo(BigInteger.ZERO) < 0 || value.compareTo(BigInteger.valueOf(Character.MAX_VALUE)) > 0) {
            throw new FutureRepresentationError(Interned.CHARACTER_CODE_REPRESENTATION);
        }
        return (char) value.intValue();
    }

    /**
     * Representation as an arity
     *
     * @return arity value
     */
    public int toArity(Environment environment) {
        long maxArity = environment.getFlags().maxArity;
        if (value.compareTo(BigInteger.ZERO) < 0 || value.compareTo(BigInteger.valueOf(maxArity)) > 0) {
            throw new FutureRepresentationError(Interned.MAX_ARITY_REPRESENTATION);
        }
        return (char) value.intValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologInteger notLessThanZero() {
        if (value.compareTo(BigInteger.ZERO) < 0) {
            throw new FutureDomainError(Interned.NOT_LESS_THAN_ZERO_DOMAIN, this);
        }
        return this;
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
    @Override
    public PrologInteger power(PrologNumber right) {
        int rightInt = right.toPrologInteger().notLessThanZero().toInteger();
        return new PrologInteger(value.pow(rightInt));
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
        if (cmp > 0) {
            return new PrologInteger(leftVal.mod(rightVal));
        } else if (cmp < 0) {
            return new PrologInteger(leftVal.mod(rightVal.negate()).negate());
        } else {
            throw new FutureEvaluationError(Interned.ZERO_DIVISOR_EVALUATION, "Division by zero");
        }
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
        } else if (!value.isInstantiated()) {
            throw new FutureInstantiationError(value);
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
        return get().compareTo(((PrologInteger) o).get());
    }
}
