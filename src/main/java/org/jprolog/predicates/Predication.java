// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.predicates;

import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.DeferredCompoundTerm;
import org.jprolog.expressions.Term;
import org.jprolog.constants.PrologAtomInterned;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.constants.PrologInteger;
import org.jprolog.execution.Environment;

/**
 * A predication is defined by the functor and arity.
 */
public class Predication implements Comparable<Predication> {
    public static final Predication UNDEFINED = new Predication(org.jprolog.bootstrap.Interned.UNKNOWN_ATOM, 0);
    private final PrologAtomLike functor;
    private final int arity;

    /**
     * Create a predication from Functor and Arity
     *
     * @param functor Functor atom
     * @param arity   Arity - 0 indicates simple atom, 1 or more indicates compound term
     */
    public Predication(PrologAtomLike functor, int arity) {
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
     * Intern a predication
     *
     * @param environmentShared Shared execution environment
     * @return interned predication
     */
    public Predication.Interned intern(Environment.Shared environmentShared) {
        return new Predication.Interned(PrologAtomInterned.from(environmentShared, functor), arity);
    }

    /**
     * @return Functor
     */
    public PrologAtomLike functor() {
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
                org.jprolog.bootstrap.Interned.SLASH_ATOM,
                2,
                () -> new Term[]{
                        functor,
                        PrologInteger.from(arity)
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
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Predication o) {
        // Follows Prolog rules of order, see {@link CompoundTerm).
        int cmp = Integer.compare(arity, o.arity);
        if (cmp != 0) {
            return cmp;
        }
        return functor.compareTo(o.functor);
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
