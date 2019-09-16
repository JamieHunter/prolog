// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;

/**
 * Indirect call to a predicate. If any paths lead to success, a cut is performed ensuring it is only
 * called once.
 */
public class ExecOnce extends ExecCall {

    public ExecOnce(Instruction once) {
        super(once);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void preCall(Environment environment) {
        // protective cut-scope for the expression being called
        environment.setExecution(new EndOnceScope(environment));
    }

    /**
     * Version of {@link ConstrainedCutPoint} IP to cut decision points at end of successful
     * call on return.
     */
    private static class EndOnceScope extends ConstrainedCutPoint {

        EndOnceScope(Environment environment) {
            super(environment);
            //markDecisionPoint();
        }

        @Override
        public void invokeNext() {
            cut();
            super.invokeNext();
        }
    }

}
