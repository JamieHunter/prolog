// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

import java.util.ListIterator;

/**
 * An entry on the backtracking stack.
 */
public interface Backtrack {
    /**
     * Called to perform the backtracking step.
     */
    default void backtrack() {
        undo();
    }

    /**
     * Called during cut pruning. Iter can be used to operate on the entry to delete the backtracking entry.
     * The goal is to reduce as many entries as possible in response to a cut.
     */
    default void cut(ListIterator<Backtrack> iter) {
        // does nothing
    }

    /**
     * Called to undo state (trimming, backtracking, throwing)
     */
    void undo();
}
