// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.unification;

import org.jprolog.constants.Atomic;
import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.Term;

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
        Term other = it.next().resolve(context);
        if (!other.isInstantiated() && other.instantiate(atomic)) {
            return it;
        }
        if (other.isAtomic() && atomic.compareTo(other) == 0) {
            return it;
        }
        if (atomic.value().compareTo(other.value()) == 0) {
            return it;
        }
        return UnifyIterator.FAILED;
    }
}
