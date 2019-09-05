// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.expressions;

import prolog.expressions.Term;

/**
 * A container term is a term that requires value() to be called to obtain a value that can be inspected or unified.
 * In particular, variables are containers.
 */
public interface Container extends Term {

    /**
     * Each Term type is given a rank per Prolog standard,
     * not expected to be called on container terms.
     *
     * @return rank of term type
     */
    @Override
    default int typeRank() {
        throw new UnsupportedOperationException("Unexpected");
    }

    /**
     * Compare primitive values, not expected to be called on container terms.
     *
     * @param o Other value
     * @return rank order
     */
    @Override
    default int compareSameType(Term o) {
        throw new UnsupportedOperationException("Unexpected");
    }
}
