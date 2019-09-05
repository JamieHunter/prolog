// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.unification;

import prolog.constants.Atomic;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Utility to build a suitable unifier for the given term.
 */
public final class UnifyBuilder {

    private final List<UnifyStep> compiler = new ArrayList<>();
    private final Function<Term, UnifyIterator> startIterator;

    private UnifyBuilder(Function<Term, UnifyIterator> startIterator) {
        this.startIterator = startIterator;
    }

    /**
     * Construct a unifier from provided term.
     *
     * @param term Term to construct unifier from
     * @return Unifier
     */
    public static Unifier from(Term term) {
        if (term instanceof CompoundTerm) {
            // Compound term unifier
            return new UnifyBuilder(UnifyBuilder::getCompoundIterator)
                    .compound((CompoundTerm) term)
                    .construct();
        } else {
            // Single term unifier
            return new UnifyBuilder(UnifyBuilder::getSingleTerm)
                    .single(term)
                    .construct();
        }
    }

    /**
     * Compile unifier for a compound term.
     *
     * @param term compound term
     * @return self
     */
    private UnifyBuilder compound(CompoundTerm term) {
        // Start iterator optimized for iterating a compound term
        // Iterate this compound term
        UnifyIterator it = getCompoundIterator(term);
        // Treat head specially (functor, potentially first term)
        unifyHead(it);
        // Unify remaining terms of compound term
        unifyLoop(it);
        return this;
    }

    /**
     * Compile unifier for atomic term or variable.
     *
     * @param term Term to compile for unification
     * @return self
     */
    private UnifyBuilder single(Term term) {
        // Start iterator assumes single term
        unify(term);
        return this;
    }

    /**
     * Recursive build unification for this term
     *
     * @param term Term to unify
     */
    private void unify(Term term) {
        if (term instanceof CompoundTerm) {
            add(new UnifyCompound((CompoundTerm) term));
        } else if (!term.isInstantiated()) {
            add(new UnifyVariable((Variable) term));
        } else {
            add(new UnifyAtomic((Atomic) term));
        }
    }

    /**
     * Unify functor/arity of a compound term via iterator
     *
     * @param it Iterator
     */
    private void unifyHead(UnifyIterator it) {
        if (it.listNext()) {
            // If compound term is a list, quick unify with list head
            add(UnifyListHead.STEP);
        } else {
            // any other compound term has a more generic validation
            add(new UnifyFunctorArity((Atomic) it.next(), it.size()));
        }
    }

    /**
     * Unify all terms in a compound term
     *
     * @param it Iterator
     */
    private void unifyLoop(UnifyIterator it) {
        while (!it.done()) {
            Term t = it.next();
            if (t instanceof CompoundTerm && it.done()) {
                // tail optimization
                add(new UnifyCompoundTail((CompoundTerm) t));
                it = getCompoundIterator(t);
                unifyHead(it);
                continue;
            }
            unify(t);
        }
    }

    /**
     * Construct the final unifier
     *
     * @return Unifier
     */
    private Unifier construct() {
        return new UnifyRunner(compiler, startIterator);
    }

    /**
     * Compile a step to set of unification steps
     *
     * @param step Step to compile
     */
    private void add(UnifyStep step) {
        this.compiler.add(step);
    }

    /**
     * Iterator to use if iterating a compound term.
     *
     * @param other Term to iterate
     * @return iterator
     */
    private static UnifyIterator getCompoundIterator(Term other) {
        if (other instanceof CompoundTerm) {
            return ((CompoundTerm) other).getUnifyIterator();
        } else {
            return UnifyIterator.FAILED;
        }
    }

    /**
     * Iterator to use for a single value term
     *
     * @param other Term to iterate
     * @return iterator
     */
    private static UnifyIterator getSingleTerm(Term other) {
        return new SingleTermIterator(other);
    }

}
