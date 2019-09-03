// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

import java.util.ListIterator;

/**
 * This is a decision point partial implementation. Backtracking pauses at each decision point to consider
 * alternatives.
 */
public abstract class DecisionPointImpl implements DecisionPoint {

    private final InstructionPointer[] stack;
    protected final Environment environment;
    protected final LocalContext decisionContext;
    protected final CatchPoint catchPoint;
    protected final CutPoint cutPoint;

    /**
     * Create a new decision point associated with the environment. At time decision point is created, the local context,
     * the catch point, the cut depth and the call stack are all snapshot and reused on each iteration of the decision
     * point.
     *
     * @param environment Execution environment
     */
    protected DecisionPointImpl(Environment environment) {
        this.environment = environment;
        this.decisionContext = environment.getLocalContext();
        this.catchPoint = environment.getCatchPoint();
        this.cutPoint = environment.getCutPoint();
        this.stack = environment.constructStack();
    }

    /**
     * Restore state to that at the time the decision point was first executed.
     */
    public void restore() {
        environment.setCutPoint(cutPoint);
        environment.setCatchPoint(catchPoint);
        environment.setLocalContext(decisionContext);
        environment.restoreStack(stack);
    }

    /**
     * Called during backtracking process
     */
    @Override
    public void backtrack() {
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
