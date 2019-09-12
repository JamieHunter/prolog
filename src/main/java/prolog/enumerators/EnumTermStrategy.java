// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.enumerators;

import prolog.constants.AtomicBase;
import prolog.constants.PrologAtomLike;
import prolog.constants.PrologStringAsList;
import prolog.exceptions.PrologThrowable;
import prolog.execution.Environment;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Container;
import prolog.expressions.Term;
import prolog.expressions.TermList;
import prolog.variables.UnboundVariable;
import prolog.variables.Variable;

import java.util.HashMap;
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
    protected final Map<Long, Variable> varMap = new HashMap<>();

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
     * @param container Prolog container term
     * @return replacement term
     */
    public Term visitContainer(Container container) {
        Term contained = container.extract();
        if (contained != container) {
            return contained.enumTerm(this);
        } else {
            return contained;
        }
    }

    /**
     * Utility - call to rename a variable, cached in variable map.
     *
     * @param source Source variable
     * @return Renamed variable (deduped)
     */
    protected Variable renameVariable(Variable source) {
        return varMap.computeIfAbsent(source.id(), id -> new UnboundVariable(source.name(), environment.nextVariableId()));
    }

    /**
     * Utility - call to unbind a variable, cached in variable map.
     *
     * @param source Source variable
     * @return Unbound variable (deduped)
     */
    protected Variable unbindVariable(Variable source) {
        return varMap.computeIfAbsent(source.id(), id -> new UnboundVariable(source.name(), id));
    }

    /**
     * Utility - call to bind a variable, cached in variable map.
     *
     * @param source Source variable
     * @return Bound variable (deduped)
     */
    protected Variable bindVariable(Variable source) {
        return varMap.computeIfAbsent(source.id(), id -> environment.getLocalContext().bind(source.name(), id));
    }

    /**
     * Utility - simply add the variable
     *
     * @param source Source variable
     * @return variable
     */
    protected Variable addVariable(Variable source) {
        return varMap.computeIfAbsent(source.id(), id -> source);
    }

    /**
     * Utility - true if variable has already been seen
     *
     * @param source Source variable
     * @return true if already seen
     */
    protected boolean hasVariable(Variable source) {
        return varMap.containsKey(source.id());
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
