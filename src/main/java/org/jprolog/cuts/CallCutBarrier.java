// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.cuts;

import org.jprolog.execution.Environment;

/**
 * A cut handler that wraps a localized call, changes cut scope, and performs a cut.
 */
public class CallCutBarrier implements CutPoint {
    protected final Environment environment;
    protected final CutPoint parent;
    private final int depth;

    public CallCutBarrier(Environment environment, CutPoint parent) {
        this.environment = environment;
        this.parent = parent;
        this.depth = environment.getBacktrackDepth(); // a cut will trim
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cut() {
        environment.cutBacktrackStack(depth);
        environment.setCutPoint(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDeterministic(long variableId) {
        // parent is in charge of what is considered deterministic
        return parent.isDeterministic(variableId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getWatermark() {
        return parent.getWatermark();
    }

}
