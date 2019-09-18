// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.callstack.ResumableExecutionPoint;
import org.jprolog.callstack.TransferHint;
import org.jprolog.execution.CatchPoint;
import org.jprolog.cuts.CutPoint;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.Term;
import org.jprolog.unification.Unifier;
import org.jprolog.unification.UnifyBuilder;

/**
 * Indirect call to a predicate. Add decision point to handle exceptions.
 */
public class ExecCatch extends ExecCall {

    private final Unifier unifier;
    private final Instruction recover;

    /**
     * Construct a catch instruction.
     *
     * @param callable  Call instruction.
     * @param matchTerm Term that is unified with the thrown term.
     * @param recover   Call-like instruction used if error is caught.
     */
    public ExecCatch(Instruction callable, Term matchTerm, Instruction recover) {
        super(callable);
        this.unifier = UnifyBuilder.from(matchTerm);
        this.recover = new ExecCall(recover);
    }

    /**
     * Adapts Call behavior with extra step of managing CatchHandler.
     *
     * @param environment Execution environment
     * @return IP
     */
    @Override
    protected ConstrainedCutPoint prepareCall(Environment environment) {
        LocalContext context = environment.getLocalContext();
        CatchHandler handler = new CatchHandler(environment);
        environment.setCatchPoint(handler);
        return new EndThrowScope(context, handler);
    }

    /**
     * An "IP" that restores catch scope as well as all other behaviors of
     * ConstrainedCutPoint. This can be optimized away per call semantics.
     */
    private static class EndThrowScope extends ConstrainedCutPoint {

        final CatchHandler handler;

        EndThrowScope(LocalContext context, CatchHandler handler) {
            super(context.environment());
            this.handler = handler;
        }

        @Override
        public void invokeNext() {
            handler.endCall();
            super.invokeNext();
        }
    }

    /**
     * Actual catch handler associated with this predicate.
     */
    private class CatchHandler extends CatchPoint {

        private final ResumableExecutionPoint executionPoint;
        final LocalContext catchContext;
        final CutPoint cut;
        final CatchPoint parent;
        final int dataStackDepth;
        final int backtrackDepth;

        CatchHandler(Environment environment) {
            // Capture state that needs to be restored
            this.catchContext = environment.getLocalContext();
            this.parent = environment.getCatchPoint();
            this.cut = environment.getCutPoint();
            this.backtrackDepth = environment.getBacktrackDepth();
            // if Java exception occurred, data stack may be invalid
            this.dataStackDepth = environment.getDataStackDepth();
            this.executionPoint = environment.getExecution().freeze();
        }

        /**
         * Handle call completion on success.
         */
        void endCall() {
            catchContext.environment().setCatchPoint(parent);
        }

        /**
         * Handle a throw. Either the throw is handled, or this handler is disguarded.
         *
         * @param thrown Thrown term
         * @return true if handled
         */
        @Override
        public boolean tryCatch(Term thrown) {
            Environment environment = catchContext.environment();
            thrown = thrown.value();
            environment.setCatchPoint(parent); // next catch point
            // forced backtrack to here - has to be done prior to unify
            environment.trimBacktrackStackToDepth(backtrackDepth);
            environment.setLocalContext(catchContext); // context for this catch
            environment.setCutPoint(cut); // restore related cut point / deterministic state
            if (!unifier.unify(catchContext, thrown)) {
                // unify failed, drop to next handler
                return false;
            }
            // unify succeeded. This catch will handle the throw
            // Complete restoration of state.
            environment.trimDataStack(dataStackDepth); // if system error
            environment.setExecution(executionPoint, TransferHint.CATCH); // resume execution
            environment.forward();
            // Now resume via the recover block
            recover.invoke(environment);
            return true;
        }
    }

}
