// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.unification;

import org.jprolog.constants.PrologEmptyList;
import org.jprolog.expressions.Term;

/**
 * This iterator is used to wrap a single term
 */
public final class SingleTermIterator implements UnifyIterator {
    private final Term term;
    private static final int TERM = 0;
    private static final int DONE = 1;
    private static final int FAILED = 2;
    private int i = TERM;

    /**
     * Construct a single term iterator
     *
     * @param term Term to iterate.
     */
    public SingleTermIterator(Term term) {
        this.term = term;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term next() {
        switch (i++) {
            case TERM:
                return term;
            case DONE:
                return PrologEmptyList.EMPTY_LIST;
            default:
                i = FAILED;
                return PrologEmptyList.EMPTY_LIST;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean done() {
        return i >= DONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean success() {
        return i == DONE;
    }
}
