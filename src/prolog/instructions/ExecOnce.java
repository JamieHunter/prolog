// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.expressions.Term;

/**
 * Indirect call to a predicate. If any paths lead to success, a cut is performed ensuring it is only
 * called once.
 */
public class ExecOnce extends ExecCall {

    public ExecOnce(Environment environment, Term notProvableTerm) {
        super(environment, notProvableTerm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void preCall() {
        // protective cut-scope for the expression being called
        LocalContext context = environment.getLocalContext();
        environment.callIP(new EndOnceScope(context));
    }

    /**
     * Version of {@link prolog.instructions.ExecCall.RestoreCutDepth} IP to cut decision points at end of successful
     * call on return.
     */
    private static class EndOnceScope extends RestoreCutDepth {

        EndOnceScope(LocalContext context) {
            super(context);
        }

        @Override
        public void next() {
            context.environment().cutDecisionPoints();
            super.next();
        }
    }

}
