// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.unification;

import prolog.constants.PrologEmptyList;
import prolog.expressions.Term;

/**
 * This iterator is used for compound terms and for simple lists.
 */
public final class SimpleUnifyIterator implements UnifyIterator {
    private final Term[] members;
    private int i;
    private final int size;

    /**
     * Construct an iterator for an array of terms
     *
     * @param size    Reported size (e.g. arity)
     * @param members Array of members to iterate
     */
    public SimpleUnifyIterator(int size, Term[] members) {
        this.size = size;
        this.i = 0;
        this.members = members;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term next() {
        if (i >= members.length) {
            i = members.length + 1;
            return PrologEmptyList.EMPTY_LIST;
        } else {
            return members[i++];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean done() {
        return i >= members.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean success() {
        return i == members.length;
    }
}
