// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.debugging;

import org.jprolog.execution.Backtrack;
import org.jprolog.execution.DecisionPoint;
import org.jprolog.execution.Environment;

import java.util.ListIterator;

public class DebugDecisionPoint implements DecisionPoint {

    private final Environment environment;
    private final DecisionPoint decisionPoint;
    private final Scoped redoScope;

    public DebugDecisionPoint(Environment environment, DecisionPoint decisionPoint, Scoped redoScope) {
        this.environment = environment;
        this.decisionPoint = decisionPoint;
        this.redoScope = redoScope;
    }

    @Override
    public void backtrack() {
        // explicitly call restore & next, to allow wrapping around next.
        restore();
        redo();
    }

    @Override
    public void cut(ListIterator<Backtrack> iter) {
        decisionPoint.cut(iter);
    }

    @Override
    public void restore() {
        decisionPoint.restore(); // restores context to just before decision point
    }

    @Override
    public void redo() {
        environment.debugger().redo(this, decisionPoint);
    }

    @Override
    public void undo() {
        decisionPoint.undo();
    }

    public Scoped getScope() {
        return redoScope;
    }
}
