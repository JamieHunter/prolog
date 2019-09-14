// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.unification;

import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.Term;

/**
 * A unifier for a nested compound/list term
 */
public final class UnifyCompound implements UnifyStep {

    private final CompoundTerm nested;
    private final Unifier unifier;

    /**
     * Create unifier for nested compound term
     *
     * @param nested Compound term
     */
    /*package*/
    UnifyCompound(CompoundTerm nested) {
        this.nested = nested;
        this.unifier = UnifyBuilder.from(nested);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnifyIterator invoke(LocalContext context, UnifyIterator it) {
        Term other = it.next();
        if (!other.isInstantiated()) {
            // other was a variable, make sure structure of nested is resolved before instantiation
            CompoundTerm resolved = nested.resolve(context);
            if (other.instantiate(resolved)) {
                return it;
            }
        }
        if (unifier.unify(context, other)) {
            return it;
        }
        return UnifyIterator.FAILED;
    }
}
