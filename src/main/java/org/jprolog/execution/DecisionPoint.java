// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.execution;

/**
 * This is a decision point as an interface. Backtracking pauses at each decision point to consider
 * alternatives.
 */
public interface DecisionPoint extends Backtrack {

    /**
     * Restore state prior to moving to next decision point.
     */
    void restore();

    /**
     * Move to next decision point.
     */
    void redo();
}
