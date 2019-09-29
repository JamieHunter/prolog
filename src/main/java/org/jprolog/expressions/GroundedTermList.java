// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.expressions;

import org.jprolog.constants.Grounded;
import org.jprolog.enumerators.EnumTermStrategy;
import org.jprolog.execution.LocalContext;

import java.util.List;

/**
 * TermList after resolving to a grounded term list.
 */
/*package*/ class GroundedTermList extends TermListImpl implements Grounded {
    protected GroundedTermList(List<Term> terms, Term tailTerm) {
        super(terms, tailTerm);
    }

    /**
     * @return true, all variables are grounded
     */
    @Override
    public boolean isGrounded() {
        return true;
    }

    /**
     * Resolve to a grounded TermList
     *
     * @param context not used
     * @return self, term is grounded
     */
    @Override
    public TermList resolve(LocalContext context) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    protected TermListImpl newList(List<Term> terms, Term tail) {
        return new GroundedTermList(terms, tail);
    }

    @Override
    public TermList enumTerm(EnumTermStrategy strategy) {
        if (strategy.pruneGroundedCompound()) {
            return this;
        } else {
            return super.enumTerm(strategy);
        }
    }
}
