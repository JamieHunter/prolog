// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

/**
 * A simple cut handler that rolls back backtracking stack.
 */
public class BasicCutPoint implements CutPoint {

    private static final int UNKNOWN = -1;
    protected final Environment environment;
    protected final CutPoint parent;
    private int depth = UNKNOWN;

    public BasicCutPoint(Environment environment, CutPoint parent) {
        this.environment = environment;
        this.parent = parent;
    }

    /**
     * Cut, perform a cut across the backtrack stack for the scope of this cut point.
     */
    @Override
    public void cut() {
        if (depth != UNKNOWN) {
            environment.cutBacktrackStack(depth);
            depth = UNKNOWN;
        }
    }

    /**
     * If backtrack depth not yet recorded, capture it as location of a decision point
     */
    @Override
    public void markDecisionPoint(int depth) {
        if (this.depth == UNKNOWN) {
            this.depth = depth;
            parent.markDecisionPoint(depth); // this effects parent too
        }
    }

    /**
     * Deterministic if depth is unknown, or if stack has been rolled-back
     *
     * @return true if execution described by this scope is deterministic
     */
    @Override
    public boolean isDeterministic() {
        return depth == UNKNOWN || depth >= environment.getBacktrackDepth();
    }
}
