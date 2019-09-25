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
    protected final int backtrackMark;

    public ClauseCutBarrier(Environment environment, CutPoint parent) {
        this(environment, parent, environment.variableWatermark());
    }

    public ClauseCutBarrier(Environment environment, CutPoint parent, long watermark) {
        this.environment = environment;
        this.parent = parent;
        this.watermark = watermark;
        this.backtrackMark = environment.getBacktrackDepth();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cut() {
        // throw away items in backtrack stack that are not needed
        environment.cutBacktrackStack(backtrackMark);
        // throw away any inner cut points
        environment.setCutPoint(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDeterministic(long variableId) {
        if (variableId >= watermark) {
            // all variables in this cut-scope may be deterministic (don't write a trace)
            // only if nothing has been added to the backtrack stack.
            return this.backtrackMark == environment.getBacktrackDepth();
        } else {
            // earlier variables must be delegated so that they are not accidentally considered as deterministic
            return parent.isDeterministic(variableId);
        }
    }

    @Override
    public long getWatermark() {
        return watermark;
    }
}
