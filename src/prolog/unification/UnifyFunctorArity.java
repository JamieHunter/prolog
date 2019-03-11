// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.unification;

import prolog.constants.Atomic;
import prolog.execution.LocalContext;

/**
 * A unifier that assumes the target functor has already been normalized.
 */
public final class UnifyFunctorArity implements UnifyStep {

    private final Atomic functor;
    private final int arity;

    /*package*/
    UnifyFunctorArity(Atomic functor, int arity) {
        this.functor = functor;
        this.arity = arity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnifyIterator invoke(LocalContext context, UnifyIterator it) {
        if (it.next() == functor && it.size() == arity) {
            return it;
        } else {
            return UnifyIterator.FAILED;
        }
    }
}
