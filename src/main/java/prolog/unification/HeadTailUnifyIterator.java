// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.unification;

import prolog.bootstrap.Interned;
import prolog.constants.PrologEmptyList;
import prolog.expressions.Term;
import prolog.expressions.TermList;

/**
 * This iterator is used for lists acting as compound terms.
 */
public final class HeadTailUnifyIterator implements UnifyIterator {
    private final TermList list;
    private static final int FUNCTOR = 0;
    private static final int HEAD = 1;
    private static final int TAIL = 2;
    private static final int DONE = 3;
    private static final int FAILED = 4;
    private int i = FUNCTOR;


    public HeadTailUnifyIterator(TermList list) {
        this.list = list;
    }

    /**
     * Immediately visit head
     *
     * @return true indicating HEAD is next to be visited
     */
    @Override
    public boolean listNext() {
        i = HEAD;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term next() {
        switch (i++) {
            case FUNCTOR:
                return Interned.LIST_FUNCTOR;
            case HEAD:
                return list.getHead();
            case TAIL:
                return list.getTail();
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
        return 2;
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
