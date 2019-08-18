// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.predicates;

import prolog.constants.Atomic;
import prolog.constants.PrologAtomInterned;
import prolog.constants.PrologInteger;
import prolog.execution.Environment;
import prolog.expressions.CompoundTerm;
import prolog.expressions.DeferredCompoundTerm;
import prolog.expressions.Term;

import java.math.BigInteger;

/**
 * A predication is defined by the functor and arity.
 */
public class Predication {
    public static final Predication UNDEFINED = new Predication(prolog.bootstrap.Interned.UNKNOWN_ATOM, 0);
    private final Atomic functor;
    private final int arity;

    /**
     * Create a predication from Functor and Arity
     *
     * @param functor Functor atom
     * @param arity   Arity - 0 indicates simple atom, 1 or more indicates compound term
     */
    public Predication(Atomic functor, int arity) {
        this.functor = functor;
        this.arity = arity;
    }

    /**
     * Intern a predication
     *
     * @param environment Execution environment
     * @return interned predication
     */
    public Predication.Interned intern(Environment environment) {
        return new Predication.Interned(PrologAtomInterned.from(environment, functor), arity);
    }

    /**
     * @return Functor
     */
    public Atomic functor() {
        return functor;
    }

    /**
     * @return Arity
     */
    public int arity() {
        return arity;
    }

    /**
     * @return '/'(Functor,Arity)
     */
    public CompoundTerm term() {
        return new DeferredCompoundTerm(
                prolog.bootstrap.Interned.SLASH_ATOM,
                2,
                () -> new Term[]{
                        functor,
                        new PrologInteger(BigInteger.valueOf(arity))
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Predication otherPredication = (Predication) other;
        return functor == otherPredication.functor && arity == otherPredication.arity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return functor.hashCode() * 31 + arity;
    }

    /**
     * Convert to string
     *
     * @return String form of predication.
     */
    @Override
    public String toString() {
        return functor.toString() + "/" + arity;
    }

    /**
     * Predication that is guaranteed to be interned
     */
    public static class Interned extends Predication {

        public Interned(PrologAtomInterned functor, int arity) {
            super(functor, arity);
        }

        @Override
        public Interned intern(Environment environment) {
            return this;
        }

        @Override
        public PrologAtomInterned functor() {
            return (PrologAtomInterned) super.functor();
        }
    }
}
