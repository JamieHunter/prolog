// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.expressions;

import prolog.constants.Grounded;
import prolog.execution.EnumTermStrategy;
import prolog.execution.LocalContext;

/**
 * Version of CompoundTerm after resolving to a grounded compound term.
 */
/*package*/ class GroundedCompoundTerm extends CompoundTermImpl implements Grounded {
    /**
     * Construct a bound compound term. First member is the functor,
     * remaining members are the arguments.
     * @param members Functor and Arguments
     */
    public GroundedCompoundTerm(Term ... members) {
        super(members);
    }

    /**
     *
     * @return true, term is grounded.
     */
    @Override
    public boolean isGrounded() {
        return true;
    }

    /**
     * Resolve does nothing.
     * @param context not used
     * @return self, term is grounded
     */
    @Override
    public CompoundTerm resolve(LocalContext context) {
        return this;
    }

    @Override
    public CompoundTerm enumTerm(EnumTermStrategy strategy) {
        if (strategy.pruneGroundedCompound()) {
            return this;
        } else {
            return super.enumTerm(strategy);
        }
    }
}
