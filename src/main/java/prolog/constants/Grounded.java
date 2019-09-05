package prolog.constants;

import prolog.execution.LocalContext;
import prolog.expressions.Term;

/**
 * A grounded term is a term that contains no variables. All Atomics are grounded. CompoundTerm's are not grounded
 * unless all variables become grounded as part of a resolve process.
 */
public interface Grounded extends Term {

    /**
     * Indicates this term is grounded. Note that this is faster than doing "instanceof Grounded"
     * @return true indicating grounded
     */
    @Override
    default boolean isGrounded() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    default Term resolve(LocalContext context) {
        return this;
    }
}
