// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.predicates;

/**
 * Represents a predicate that does not exist
 */
public class MissingPredicate extends PredicateDefinition {

    /**
     * Singleton, used to represent a missing predicate rather than using null.
     */
    public static final MissingPredicate MISSING_PREDICATE = new MissingPredicate();

    private MissingPredicate() {
    }

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
