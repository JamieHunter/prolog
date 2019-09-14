// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.execution.CutPoint;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.execution.InstructionPointer;
import org.jprolog.execution.LocalContext;

/**
 * Perform a call with new local scope
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
        LocalContext newContext = environment.newLocalContext();
        environment.setLocalContext(newContext);
        environment.setCutPoint(newContext);
        environment.callIP(new EndLocalScope(environment));
        instruction.invoke(environment);
    }

    private static class EndLocalScope implements InstructionPointer {

        private final Environment environment;
        private final LocalContext savedContext;
        private final CutPoint savedCut;

        EndLocalScope(Environment environment) {
            this.environment = environment;
            this.savedContext = environment.getLocalContext();
            this.savedCut = environment.getCutPoint();
        }

        @Override
        public void next() {
            environment.setCutPoint(savedCut);
            environment.setLocalContext(savedContext);
            environment.restoreIP();
        }
    }

}
