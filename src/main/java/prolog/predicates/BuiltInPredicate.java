// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.predicates;

/**
 * A base class for any predicate that is built in to Prolog
 */
public abstract class BuiltInPredicate extends PredicateDefinition {
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDynamic() {
        return false;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMultifile() {
        return false;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDiscontiguous() {
        return false;
    }
}
