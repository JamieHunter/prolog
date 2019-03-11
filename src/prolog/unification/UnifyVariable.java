// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.unification;

import prolog.execution.LocalContext;
import prolog.expressions.Term;
import prolog.variables.Variable;

/**
 * Unify a variable with anything.
 */
public final class UnifyVariable implements UnifyStep {

    private final Variable variable;

    /**
     * Construct unifier step
     *
     * @param variable Variable term to unify with iterated term.
     */
    /*package*/
    UnifyVariable(Variable variable) {
        this.variable = variable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnifyIterator invoke(LocalContext context, UnifyIterator it) {
        Term other = it.next();
        Term bound = variable.resolve(context);
        if (bound.instantiate(other)) {
            return it;
        } else if (Unifier.unify(context, bound, other)) {
            return it;
        } else {
            return UnifyIterator.FAILED;
        }
    }
}
