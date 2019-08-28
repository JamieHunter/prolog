// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.bootstrap.Interned;
import prolog.debugging.InstructionReflection;
import prolog.execution.CatchPoint;
import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.LocalContext;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;

/**
 * Indirect call to a predicate. Call an instruction on success/failure/exception. Implicit cut occurs.
 * This is intended to be used internally.
 */
public class ExecFinally extends ExecCall {

    private final Instruction onBefore;
    private final Instruction onFinally;

    /**
     * Create an if-else call.
     *
     * @param environment Execution environment
     * @param callable    Term to evaluate
     * @param onBefore    Instruction to execute once hooks have been inserted
     * @param onFinally   Instruction to execute regardless of exit condition
     */
    public ExecFinally(Environment environment, Term callable,
                       Instruction onBefore,
                       Instruction onFinally) {

        super(environment,
                new CompoundTermImpl(Interned.CALL_FUNCTOR, callable),
                callable);
        this.onBefore = onBefore;
        this.onFinally = onFinally;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void preCall() {
        //
        // Prepare for IF-THEN-ELSE by establishing two hooks - the first is called after Condition is executed to
        // establish success behavior. The second is called as part of a decision point to establish failure behavior.
        //
        LocalContext context = environment.getLocalContext();
        CatchHandler catchHandler = new CatchHandler();
        environment.setCatchPoint(catchHandler);
        // A return IP will handle forward progress for the then case
        environment.callIP(new OnForward(environment, catchHandler));
        // A decision point before the "cut" will handle backtracking for the else case
        environment.pushDecisionPoint(new OnBacktrack(environment));
        // protective cut-scope for the condition expression being called
        environment.callIP(new ConstrainedCutPoint(environment));
        if (onBefore != null) {
            onBefore.invoke(environment);
        }
    }

    /**
     * Version of {@link ConstrainedCutPoint} that calls success callback.
     */
    private class OnForward extends ConstrainedCutPoint {

        private final CatchHandler catchHandler;

        OnForward(Environment environment, CatchHandler catchHandler) {
            super(environment);
            this.catchHandler = catchHandler;
        }

        @Override
        public void next() {
            // remove the decision point
            // Effectively "once", but also prevents the inversion of fail to success
            cut();
            catchHandler.restore();
            super.next();
            onFinally.invoke(environment);
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
            // NOTE, no state restored. Assume that this instruction will fail
            onFinally.invoke(environment);
        }
    }

    /**
     * Catch handler to catch exception case.
     */
    private class CatchHandler extends CatchPoint {
        final CatchPoint parent;

        CatchHandler() {
            parent = environment.getCatchPoint();
        }

        private void restore() {
            environment.setCatchPoint(parent);
        }

        @Override
        public boolean tryCatch(Term thrown) {
            // NOTE, no state restored. Assume that this instruction will continue down catch chain.
            restore();
            onFinally.invoke(environment);
            return false;
        }
    }
}
