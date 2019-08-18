// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.unification;

import prolog.bootstrap.Interned;
import prolog.execution.LocalContext;
import prolog.expressions.Term;

/**
 * Optimized testing that target is a list head, singleton.
 */
public final class UnifyListHead implements UnifyStep {

    public static final UnifyListHead STEP = new UnifyListHead();

    private UnifyListHead() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnifyIterator invoke(LocalContext context, UnifyIterator it) {
        if (it.listNext()) {
            return it; // fast path
        }
        if (it.size() == 2 && it.next().compareTo(Interned.LIST_FUNCTOR) == 0) {
            return it;
        } else {
            return UnifyIterator.FAILED;
        }
    }
}
