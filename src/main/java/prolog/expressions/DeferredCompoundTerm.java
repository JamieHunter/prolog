// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.expressions;

import prolog.constants.PrologAtomLike;
import prolog.execution.EnumTermStrategy;
import prolog.execution.LocalContext;
import prolog.io.WriteContext;
import prolog.unification.UnifyIterator;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Compound term that is late-bound. This is used for example in exception paths, where a compound term exists only
 * if it is needed.
 */
public class DeferredCompoundTerm implements CompoundTerm, Container {
    private final PrologAtomLike functor;
    private final int arity;
    private final Supplier<Term[]> supplier;
    private CompoundTerm cache;

    /**
     * Create a future compound term.
     *
     * @param functor  Functor of term
     * @param arity    Arity (as Supplier is not initially called)
     * @param supplier Supplier of arguments.
     */
    public DeferredCompoundTerm(PrologAtomLike functor, int arity, Supplier<Term[]> supplier) {
        this.functor = functor;
        this.arity = arity;
        this.supplier = supplier;
    }

    /**
     * Make term exist
     *
     * @return Actual compound term
     */
    @Override
    public CompoundTerm extract() {
        if (cache != null) {
            return cache;
        }
        cache = new CompoundTermImpl(functor, supplier.get());
        if (cache.arity() != arity) {
            throw new InternalError("Arity mismatch");
        }
        return cache;
    }

    /**
     * Resolve compound term. Term is made to exist prior to call.
     *
     * @param context binding context (used e.g. to create a bound variable)
     * @return Compound term instantiated and resolved
     */
    @Override
    public CompoundTerm resolve(LocalContext context) {
        return extract().resolve(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term enumTerm(EnumTermStrategy strategy) {
        return strategy.visitContainer(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompoundTerm enumAndCopyCompoundTermMembers(EnumTermStrategy strategy) {
        throw new UnsupportedOperationException("Call extract() first");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompoundTerm enumCompoundTermMembers(EnumTermStrategy strategy) {
        throw new UnsupportedOperationException("Call extract() first");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(WriteContext context) throws IOException {
        extract().write(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int arity() {
        return arity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologAtomLike functor() {
        return functor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term get(int i) {
        return extract().get(i);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnifyIterator getUnifyIterator() {
        return extract().getUnifyIterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int typeRank() {
        throw new UnsupportedOperationException("Unexpected");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareSameType(Term o) {
        throw new UnsupportedOperationException("Unexpected");
    }

}