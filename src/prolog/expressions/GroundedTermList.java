// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.expressions;

import prolog.constants.Grounded;
import prolog.execution.LocalContext;

/**
 * TermList after resolving to a grounded term list.
 */
/*package*/ class GroundedTermList extends TermListImpl implements Grounded {
    public GroundedTermList(int index, Term[] terms, Term tailTerm) {
        super(index, terms, tailTerm);
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
    protected TermListImpl newList(int index, Term[] terms, Term tail) {
        return new GroundedTermList(index, terms, tail);
    }
}
