// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.unification;

import prolog.execution.LocalContext;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;

/**
 * A unifier for compound terms that contain a tail compound term. Note that this is a partial unify,
 * the caller completes the unification
 */
public final class UnifyCompoundTail implements UnifyStep {

    private final CompoundTerm compound;

    /**
     * Construct a tail compound unifier.
     *
     * @param compound Tail compound term
     */
    /*package*/
    UnifyCompoundTail(CompoundTerm compound) {
        this.compound = compound;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnifyIterator invoke(LocalContext context, UnifyIterator it) {
        Term other = it.next();
        if (!it.done()) {
            // expected to be tail
            return UnifyIterator.FAILED;
        }
        if (!other.isInstantiated()) {
            // the tail was bound to a variable, make sure compound is resolved before instantiation
            CompoundTerm resolved = compound.resolve(context);
            if (other.instantiate(resolved)) {
                return UnifyIterator.COMPLETED;
            }
        }
        if (other instanceof CompoundTerm) {
            return ((CompoundTerm) other).getUnifyIterator();
        } else {
            return UnifyIterator.FAILED;
        }
    }
}
