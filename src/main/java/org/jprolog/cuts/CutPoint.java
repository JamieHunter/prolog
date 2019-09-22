// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.cuts;

/**
 * An object that handles '!' (cut)
 */
public interface CutPoint {

    /**
     * perform a cut (i.e. '!')
     */
    void cut();

    /**
     * True if variable id assignment is considered to be deterministic.
     *
     * @param variableId to test
     * @return true if deterministic
     */
    boolean isDeterministic(long variableId);

    /**
     * End of a CutPoint chain
     */
    CutPoint TERMINAL = new CutPoint() {
        @Override
        public void cut() {
        }

        @Override
        public boolean isDeterministic(long variableId) {
            return true;
        }

        @Override
        public long getWatermark() {
            return -1;
        }

    };

    /**
     * Return watermark at this point.
     * @return watermark
     */
    long getWatermark();
}
