// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;

/**
 * Indirect call to a predicate. If any paths lead to success, a cut is performed ensuring it is only
 * called once. A failure is ignored (effectively merging behavior of ExecOnce and ExecIfThenElse
 */
public class ExecIgnore extends ExecCall {

    public ExecIgnore(Environment environment, CompoundTerm source, Term onceTerm) {
        super(environment, source, onceTerm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void preCall() {
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
    private class OnBacktrack extends DecisionPoint {

        OnBacktrack(Environment environment) {
            super(environment);
        }

        @Override
        protected void next() {
            // stack is just prior to this decision point being pushed
            // remove the OnForward() entry point
            environment.restoreIP();
            environment.forward();
        }
    }

}
