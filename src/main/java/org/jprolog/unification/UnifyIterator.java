// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.unification;

import org.jprolog.constants.PrologEmptyList;
import org.jprolog.expressions.Term;

/**
 * Interface implemented by all unification iterators.
 */
public interface UnifyIterator {
    /**
     * Next term, or null if out of terms or if term is a tail
     *
     * @return Term
     */
    default Term next() {
        return PrologEmptyList.EMPTY_LIST;
    }

    /**
     * When called, and list node, skip next to the head and return true.
     * Slight optimization for lists
     *
     * @return true if at list head
     */
    default boolean listNext() {
        return false;
    }

    /**
     * Number of components for a compound term, number of elements for a list
     *
     * @return size
     */
    default int size() {
        return -1;
    }

    /**
     * @return true if either failed or success
     */
    default boolean done() {
        return false;
    }

    /**
     * @return true if success
     */
    default boolean success() {
        return false;
    }

    /**
     * Singleton iterator that always fails.
     */
    UnifyIterator FAILED = new UnifyIterator() {
        public boolean done() {
            return true;
        }
    };

    /**
     * Singleton iterator that always succeeds.
     */
    UnifyIterator COMPLETED = new UnifyIterator() {
        public boolean success() {
            return true;
        }

        public boolean done() {
            return true;
        }
    };
}
