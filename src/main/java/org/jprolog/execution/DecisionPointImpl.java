// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.execution;

import org.jprolog.callstack.ResumableExecutionPoint;
import org.jprolog.cuts.CutPoint;

import java.util.ListIterator;

/**
 * This is a decision point partial implementation. Backtracking pauses at each decision point to consider
 * alternatives.
 */
public abstract class DecisionPointImpl implements DecisionPoint {

    protected final Environment environment;
    protected final LocalContext decisionContext;
    protected final CatchPoint catchPoint;
    protected final CutPoint cutPoint;
    protected final ResumableExecutionPoint executionPoint;

    /**
     * Create a new decision point associated with the environment. At time decision point is created, the local context,
     * the catch point, the cut depth and execution are all snapshot and reused on each iteration of the decision
     * point.
     *
     * @param environment Execution environment
     */
    protected DecisionPointImpl(Environment environment) {
        this.environment = environment;
        this.decisionContext = environment.getLocalContext();
        this.catchPoint = environment.getCatchPoint();
        this.cutPoint = environment.getCutPoint();
        this.executionPoint = environment.getExecution().freeze();
    }

    /**
     * Restore state to that at the time the decision point was first executed.
     */
    public void restore() {
        environment.setCutPoint(cutPoint);
        environment.setCatchPoint(catchPoint);
        environment.setLocalContext(decisionContext);
        environment.setExecution(executionPoint);
    }

    /**
     * Called during backtracking process
     */
    @Override
    public final void backtrack() {
        restore();
        redo();
    }

    /**
     * Cut removes the decision point.
     *
     * @param iter cut iterator
     */
    @Override
    public void cut(ListIterator<Backtrack> iter) {
        iter.remove();
    }

    /**
     * Undo does nothing on a decision point.
     */
    @Override
    public void undo() {
    }
}
