// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

import prolog.constants.PrologStringAsList;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.expressions.TermList;
import prolog.variables.UnboundVariable;
import prolog.variables.Variable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Strategy for enumerating a tree of terms
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
     * Visitor that is typically used for copy-tree, de-duped.
     *
     * @param src             Original term
     * @param mappingFunction Mutation
     * @return Replacement term
     */
    public Term copyVisitor(Term src, Function<? super Term, ? extends Term> mappingFunction) {
        return refMap.computeIfAbsent(src, mappingFunction);
    }

    /**
     * Visit a single term. Mapping function is only called if term has not been seen before (copyVisitor)
     *
     * @param src             Source term
     * @param mappingFunction src -> modified src (if used)
     * @return mapped term
     */
    public abstract Term visit(Term src, Function<? super Term, ? extends Term> mappingFunction);

    /**
     * Visit a compound term - call {@link CompoundTerm#mutateCompoundTerm(EnumTermStrategy)} or
     * {@link CompoundTerm#enumCompoundTerm(EnumTermStrategy)} depending on use case.
     *
     * @param compound Compound term being visited
     * @return modified compound term
     */
    public abstract CompoundTerm visit(CompoundTerm compound);

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
     * Utility - true if variable has already been seen
     *
     * @param source Source variable
     * @return true if already seen
     */
    protected boolean hasVariable(Variable source) {
        return varMap.containsKey(source.id());
    }

    /**
     * Override to visit uninstantiated variables.
     *
     * @param variable Variable reference
     * @return Replacement term
     */
    public Term visitVariable(Variable variable) {
        return variable;
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
