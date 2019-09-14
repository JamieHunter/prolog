// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.execution;

/**
 * An object that handles '!' (cut)
 */
public interface CutPoint {

    /**
     * perform a cut (i.e. '!')
     */
    void cut();

    /**
     * Change state to indicate one or more decision points exist. May be reset by cut.
     */
    void markDecisionPoint(int depth);

    /**
     * True if local context is assumed to be deterministic.
     *
     * @return true if deterministic
     */
    boolean isDeterministic();

    /**
     * End of a CutPoint chain
     */
    CutPoint TERMINAL = new CutPoint() {
        @Override
        public void cut() {
        }

        @Override
        public void markDecisionPoint(int depth) {
        }

        @Override
        public boolean isDeterministic() {
            return true;
        }
    };
}
