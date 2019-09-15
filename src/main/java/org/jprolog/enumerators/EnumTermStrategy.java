// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.enumerators;

import org.jprolog.constants.AtomicBase;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.constants.PrologStringAsList;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.Container;
import org.jprolog.expressions.Term;
import org.jprolog.expressions.TermList;
import org.jprolog.variables.Variable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Strategy for enumerating a tree of terms. This follows a visitor pattern to allow maintaining different
 * strategies.
 */
public abstract class EnumTermStrategy {
    private final Environment environment;
    private final Map<Term, Term> refMap = new IdentityHashMap<>();

    public EnumTermStrategy(Environment environment) {
        this.environment = environment;
    }

    /**
     * Return environment for this enumerator
     *
     * @return Environment
     */
    public Environment environment() {
        return environment;
    }

    /**
     * If not overridden, computes term with caching.
     *
     * @param src             Source term
     * @param computeFunction src -> modified src (if used)
     * @return computed term
     */
    public Term computeUncachedTerm(Term src, Function<? super Term, ? extends Term> computeFunction) {
        return refMap.computeIfAbsent(src, computeFunction);
    }

    /**
     * Visit an atom term - override for, e.g., interning atoms. Default delegates to {@link #visitAtomic(AtomicBase)}.
     *
     * @param atom Atom being visited
     * @return modified atom term
     */
    public Term visitAtom(PrologAtomLike atom) {
        return visitAtomic(atom);
    }

    /**
     * Visit an atomic (non compound but instantiated) term that is not an atom (unless delegated).
     *
     * @param atomic Atomic value being visited
     * @return modified atom term
     */
    public Term visitAtomic(AtomicBase atomic) {
        return atomic;
    }

    /**
     * Visit a compound term - override for optimal visit pattern.
     *
     * @param compound Compound term being visited
     * @return modified compound term
     */
    public CompoundTerm visitCompoundTerm(CompoundTerm compound) {
        // Safe default does a deep recursion
        return compound.enumAndCopyCompoundTermMembers(this);
    }

    /**
     * Visit a variable. May mutate the variable. Default is to not touch the variable.
     *
     * @param variable Variable reference
     * @return Replacement term
     */
    public Term visitVariable(Variable variable) {
        return variable;
    }

    /**
     * Visit a container term. Safest option is to evaluate contained item.
     *
     * @param container Prolog container term
     * @return replacement term
     */
    public Term visitContainer(Container container) {
        Term contained = container.value();
        if (contained != container) {
            return contained.enumTerm(this);
        } else {
            return contained;
        }
    }

    /**
     * Override to visit code-points/chars. Normally code-points/chars are not visited.
     *
     * @param stringAsList String as list term
     * @return Replacement term
     */
    public TermList visitStringAsList(PrologStringAsList stringAsList) {
        return stringAsList;
    }

    /**
     * Override if grounded compound terms should be visited
     *
     * @return true (default) if grounded compound terms should be skipped
     */
    public boolean pruneGroundedCompound() {
        return true;
    }
}
