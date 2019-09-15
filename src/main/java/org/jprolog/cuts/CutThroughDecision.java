// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.cuts;

import org.jprolog.execution.Environment;

/**
 * This wraps a decision point, and makes scope non-deterministic
 */
public class CutThroughDecision implements CutPoint {

    protected final Environment environment;
    protected final CutPoint parent;
    private final long watermark;
    private final int depth;

    public CutThroughDecision(Environment environment, CutPoint parent, int depth) {
        this.environment = environment;
        this.parent = parent;
        this.depth = depth;
        this.watermark = parent.getWatermark();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cut() {
        environment.cutBacktrackStack(depth);
        parent.cut();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDeterministic(long variableId) {
        return false;
    }

    @Override
    public long getWatermark() {
        return watermark;
    }

    @Override
    public boolean handlesDecisionPoint() {
        return true; // allows reduction of number of entries
    }
}
