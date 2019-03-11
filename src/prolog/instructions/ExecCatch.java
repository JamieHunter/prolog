// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.execution.CatchPoint;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.InstructionPointer;
import prolog.execution.LocalContext;
import prolog.expressions.Term;
import prolog.unification.Unifier;
import prolog.unification.UnifyBuilder;

/**
 * Indirect call to a predicate. Add decision point to handle exceptions.
 */
public class ExecCatch extends ExecCall {

    private final Unifier unifier;
    private final Instruction recover;

    /**
     * Construct a catch instruction.
     *
     * @param environment Execution environment this instruction is bound to.
     * @param callTerm    Call term, per {@link ExecCall}.
     * @param matchTerm   Term that is unified with the thrown term.
     * @param recoverTerm Call-like term used if an error was caught, per {@link ExecCall}.
     */
    public ExecCatch(Environment environment, Term callTerm, Term matchTerm, Term recoverTerm) {
        super(environment, callTerm);
        this.unifier = UnifyBuilder.from(matchTerm);
        this.recover = new ExecCall(environment, recoverTerm);
    }

    /**
     * Adapts Call behavior with extra step of managing CatchHandler.
     *
     * @return IP
     */
    @Override
    protected RestoreCutDepth prepareCall() {
        LocalContext context = environment.getLocalContext();
        CatchHandler handler = new CatchHandler();
        environment.setCatchPoint(handler);
        return new EndThrowScope(context, handler);
    }

    /**
     * An "IP" that restores catch scope as well as all other behaviors of
     * RestoreCutDepth. This can be optimized away per call semantics.
     */
    private static class EndThrowScope extends RestoreCutDepth {

        final CatchHandler handler;

        EndThrowScope(LocalContext context, CatchHandler handler) {
            super(context);
            this.handler = handler;
        }

        @Override
        public void next() {
            handler.endCall();
            super.next();
        }
    }

    /**
     * Actual catch handler associated with this predicate.
     */
    private class CatchHandler extends CatchPoint {

        private final InstructionPointer[] stack;
        final LocalContext catchContext;
        final CatchPoint parent;
        final int dataStackDepth;
        final int backtrackDepth;

        CatchHandler() {
            // Capture state that needs to be restored
            this.catchContext = environment.getLocalContext();
            this.parent = environment.getCatchPoint();
            this.backtrackDepth = environment.getBacktrackDepth();
            // if Java exception occurred, data stack may be invalid
            this.dataStackDepth = environment.getDataStackDepth();
            this.stack = environment.constructStack();
        }

        /**
         * Handle call completion on success.
         */
        void endCall() {
            environment.setCatchPoint(parent);
        }

        /**
         * Handle a throw. Either the throw is handled, or this handler is disguarded.
         *
         * @param thrown Thrown term
         * @return true if handled
         */
        @Override
        public boolean tryCatch(Term thrown) {
            thrown = thrown.value(environment);
            environment.setCatchPoint(parent); // next catch point
            // forced backtrack to here - has to be done prior to unify
            environment.trimBacktrackStackToDepth(backtrackDepth);
            environment.setLocalContext(catchContext); // context for this catch
            if (!unifier.unify(catchContext, thrown)) {
                // unify failed, drop to next handler
                return false;
            }
            // unify succeeded. This catch will handle the throw
            // Complete restoration of state.
            environment.trimDataStack(dataStackDepth); // if system error
            environment.restoreStack(stack); // call stack
            environment.forward();
            // Now resume via the recover block
            recover.invoke(environment);
            return true;
        }
    }

}
