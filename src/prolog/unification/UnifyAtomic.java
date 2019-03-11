// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.unification;

import prolog.constants.Atomic;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.expressions.Term;

/**
 * A test to unify an atomic constant
 */
public final class UnifyAtomic implements UnifyStep {

    private final Atomic atomic;

    /**
     * Construct a unifier for atomic terms
     *
     * @param atomic Atomic term
     */
    public UnifyAtomic(Atomic atomic) {
        this.atomic = atomic;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnifyIterator invoke(LocalContext context, UnifyIterator it) {
        Term other = it.next();
        if (other.instantiate(atomic)) {
            return it;
        }
        if (atomic.equals(other)) {
            return it;
        }
        Environment env = context.environment();
        if (atomic.value(env).equals(other.value(env))) {
            return it;
        }
        return UnifyIterator.FAILED;
    }
}
