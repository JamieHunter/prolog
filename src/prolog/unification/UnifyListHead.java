// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.unification;

import prolog.bootstrap.Interned;
import prolog.execution.LocalContext;

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
            return it;
        }
        if (it.next() == Interned.LIST_FUNCTOR && it.size() == 2) {
            return it;
        } else {
            return UnifyIterator.FAILED;
        }
    }
}
