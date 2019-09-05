// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.execution.DecisionPointImpl;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.LocalContext;

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
        environment.callIP(new EndIgnoreScope(environment));
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
        public void next() {
            cut();
            super.next();
        }
    }

    /**
     * DecisionPoint to handle failure case.
     */
    private class OnBacktrack extends DecisionPointImpl {

        OnBacktrack(Environment environment) {
            super(environment);
        }

        @Override
        public void redo() {
            // stack is just prior to this decision point being pushed
            // remove the OnForward() entry point
            environment.restoreIP();
            environment.forward();
        }
    }

}
