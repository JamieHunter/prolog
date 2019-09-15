// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.cuts;

import org.jprolog.execution.Environment;

/**
 * A cut handler that acts as a barrier and strict scope (with LocalContext)
 */
public class ClauseCutBarrier implements CutPoint {

    protected final Environment environment;
    protected final CutPoint parent;
    protected final long watermark;

    public ClauseCutBarrier(Environment environment, CutPoint parent, long watermark) {
        this.environment = environment;
        this.parent = parent;
        this.watermark = watermark;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cut() {
        // prune cut point to this barrier
        environment.setCutPoint(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDeterministic(long variableId) {
        if (variableId >= watermark) {
            // all variables in this cut-scope are deterministic (don't write a trace)
            return true;
        } else {
            // earlier variables must be delegated
            return parent.isDeterministic(variableId);
        }
    }

    @Override
    public long getWatermark() {
        return watermark;
    }

    @Override
    public boolean handlesDecisionPoint() {
        return false;
    }
}
