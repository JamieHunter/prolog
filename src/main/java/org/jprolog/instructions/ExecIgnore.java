// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.callstack.TransferHint;
import org.jprolog.execution.DecisionPointImpl;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.execution.LocalContext;

/**
 * Indirect call to a predicate. If any paths lead to success, a cut is performed ensuring it is only
 * called once. A failure is ignored (effectively merging behavior of ExecOnce and ExecIfThenElse
 */
public class ExecIgnore extends ExecCall {

    public ExecIgnore(Instruction once) {
        super(once);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void preCall(Environment environment) {
        // protective cut-scope for the expression being called
        LocalContext context = environment.getLocalContext();
        environment.setExecution(new EndIgnoreScope(environment), TransferHint.CONTROL);
        // A decision point before the "cut" will handle backtracking to effect an ignore
        environment.pushDecisionPoint(new OnBacktrack(environment));
    }

    /**
     * Version of {@link ConstrainedCutPoint} IP to cut decision points at end of successful
     * call on return.
     */
    private static class EndIgnoreScope extends ConstrainedCutPoint {

        EndIgnoreScope(Environment environment) {
            super(environment);
        }

        @Override
        public void invokeNext() {
            cut();
            super.invokeNext();
        }
    }

    /**
     * DecisionPoint to handle failure case.
     */
    private static class OnBacktrack extends DecisionPointImpl {

        OnBacktrack(Environment environment) {
            super(environment);
        }

        @Override
        public void redo() {
            // stack is just prior to this decision point being pushed
            // remove the OnForward() entry point
            environment.setExecution(environment.getExecution().previousExecution(), TransferHint.REDO);
            environment.forward();
        }
    }

}
