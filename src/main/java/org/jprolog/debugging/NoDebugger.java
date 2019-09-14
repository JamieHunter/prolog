// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.debugging;

import org.jprolog.execution.CompileContext;
import org.jprolog.execution.DecisionPoint;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.execution.InstructionPointer;

/**
 * Stub that is used when debugger is inactive
 */
public class NoDebugger implements DebuggerHook {

    public static final NoDebugger SELF = new NoDebugger();

    private NoDebugger() {

    }

    @Override
    public void trace() {

    }

    @Override
    public void invoke(Environment environment, DebugInstruction context, Instruction instruction) {
        instruction.invoke(environment);
    }

    @Override
    public void redo(DebugDecisionPoint context, DecisionPoint decisionPoint) {
        decisionPoint.redo();
    }

    @Override
    public DecisionPoint acceptDecisionPoint(DecisionPoint decisionPoint) {
        return decisionPoint;
    }

    @Override
    public void acceptIP(InstructionPointer ip) {
    }

    @Override
    public void leaveIP(InstructionPointer ip) {
    }

    @Override
    public CompileContext newCompileContext(Environment.Shared shared) {
        return new CompileContext(shared);
    }
}
