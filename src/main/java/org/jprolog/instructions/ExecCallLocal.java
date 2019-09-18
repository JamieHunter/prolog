// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.callstack.ImmutableExecutionPoint;
import org.jprolog.callstack.ResumableExecutionPoint;
import org.jprolog.callstack.TransferHint;
import org.jprolog.cuts.ClauseCutBarrier;
import org.jprolog.cuts.CutPoint;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.execution.LocalContext;

/**
 * Perform a call with new local scope, used when there is expected to be a read term with labeled variables.
 */
public class ExecCallLocal implements Instruction {
    protected final Environment environment;
    private final Instruction instruction;

    public ExecCallLocal(Environment environment, Instruction instruction) {
        this.environment = environment;
        this.instruction = instruction;
    }

    @Override
    public void invoke(Environment environment) {
        long watermark = environment.variableWatermark();
        ClauseCutBarrier barrier = new ClauseCutBarrier(environment, environment.getCutPoint(), watermark);
        LocalContext newContext = environment.newLocalContext();
        EndLocalScope callScope = new EndLocalScope(environment);
        environment.setLocalContext(newContext);
        environment.setCutPoint(barrier);
        environment.setExecution(callScope, TransferHint.CONTROL);
        instruction.invoke(environment);
    }

    private static class EndLocalScope implements ImmutableExecutionPoint {

        private final Environment environment;
        private final ResumableExecutionPoint previous;
        private final LocalContext savedContext;
        private final CutPoint savedCut;

        EndLocalScope(Environment environment) {
            this.environment = environment;
            this.previous = environment.getExecution().freeze();
            this.savedContext = environment.getLocalContext();
            this.savedCut = environment.getCutPoint();
        }

        @Override
        public void invokeNext() {
            environment.setCutPoint(savedCut);
            environment.setLocalContext(savedContext);
            environment.setExecution(previous, TransferHint.LEAVE);
        }

        @Override
        public Object id() {
            return this;
        }

        @Override
        public ResumableExecutionPoint previousExecution() {
            return previous;
        }
    }

}
