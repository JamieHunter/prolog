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
 * Executes a goal once. On success, executes the onSuccess instruction. On failure,
 * executes the onFailed instruction. This is also used, for example, to implement \+ (not-provable),
 * the if/then construct (X -&gt; Y), and the if/then/else construct (X -&gt; Y; Z). Note that
 * while this can be used to implement once, {@link ExecOnce} performs tail-call elimination.
 */
public class ExecIfThenElse extends ExecCall {

    private final Instruction onSuccess;
    private final Instruction onFailed;

    /**
     * Create an if-else call.
     *
     * @param condition Term to evaluate as the condition
     * @param onSuccess Success instruction
     * @param onFailed  Failure instruction
     */
    public ExecIfThenElse(Instruction condition,
                          Instruction onSuccess, Instruction onFailed) {

        super(condition);
        this.onSuccess = onSuccess;
        this.onFailed = onFailed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void preCall(Environment environment) {
        //
        // Prepare for IF-THEN-ELSE by establishing two hooks - the first is called after Condition is executed to
        // establish success behavior. The second is called as part of a decision point to establish failure behavior.
        //
        LocalContext context = environment.getLocalContext();
        // A return IP will handle forward progress for the then case
        environment.setExecution(new OnForward(environment), TransferHint.CONTROL);
        // A decision point before the "cut" will handle backtracking for the else case
        environment.pushDecisionPoint(new OnBacktrack(environment));
        // protective cut-scope for the condition expression being called
        environment.setExecution(new ConstrainedCutPoint(environment), TransferHint.CONTROL);
    }

    /**
     * Version of {@link ConstrainedCutPoint} that calls success callback.
     */
    private class OnForward extends ConstrainedCutPoint {

        OnForward(Environment environment) {
            super(environment);
        }

        @Override
        public void invokeNext() {
            // remove the ConfirmNotProvable decision point
            // Effectively "once", but also prevents the inversion of fail to success
            cut();
            super.invokeNext();
            onSuccess.invoke(environment);
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
            environment.setExecution(environment.getExecution().previousExecution(), TransferHint.LEAVE);
            environment.forward();
            onFailed.invoke(environment);
        }
    }
}
