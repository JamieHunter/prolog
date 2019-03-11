// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.expressions.Term;

import java.util.function.Consumer;

/**
 * Execute a goal, specify onSuccess/onFail callbacks. This is used, for example, to implement \+ (not-provable).
 */
public class ExecGuarded extends ExecCall {

    private final Consumer<Term> onSuccess;
    private final Consumer<Term> onFailed;

    /**
     * Create a guarded call.
     *
     * @param environment Execution environment
     * @param callTerm    Term to call
     * @param onSuccess   Success handler
     * @param onFailed    Failure handler
     */
    public ExecGuarded(Environment environment, Term callTerm,
                       Consumer<Term> onSuccess, Consumer<Term> onFailed) {

        super(environment, callTerm);
        this.onSuccess = onSuccess;
        this.onFailed = onFailed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void preCall() {
        //
        // Achieve the inversion by inserting an additional cut scope. This cut scope will
        // contain a decision point, which will invert a fail to a success.
        //
        // Conversely a success is turned into a fail.
        //
        LocalContext context = environment.getLocalContext();
        environment.callIP(new EndGuarded(context));
        // A backtrack before the "cut"
        context.pushDecision(new FailGuarded(environment));
        // protective cut-scope for the expression being called, we still need to do above decision
        environment.callIP(new RestoreCutDepth(context));
    }

    /**
     * Version of {@link prolog.instructions.ExecCall.RestoreCutDepth} that calls success callback.
     */
    private class EndGuarded extends RestoreCutDepth {

        EndGuarded(LocalContext context) {
            super(context);
        }

        @Override
        public void next() {
            Environment environment = context.environment();
            // remove the ConfirmNotProvable decision point
            // Effectively "once", but also prevents the inversion of fail to success
            environment.cutDecisionPoints();
            super.next();
            onSuccess.accept(callTerm);
        }
    }

    /**
     * DecisionPoint to handle failure case.
     */
    private class FailGuarded extends DecisionPoint {

        FailGuarded(Environment environment) {
            super(environment);
        }

        @Override
        protected void next() {
            // stack is just prior to this decision point being pushed
            // remove the EndGuarded() entry point
            environment.restoreIP();
            onFailed.accept(callTerm);
        }
    }
}
