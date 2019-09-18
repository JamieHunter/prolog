// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.debugging;

import org.jprolog.callstack.ExecutionPoint;
import org.jprolog.callstack.TransferHint;
import org.jprolog.execution.CompileContext;
import org.jprolog.execution.DecisionPoint;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;

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
    public void setExecution(ExecutionPoint ep, TransferHint hint) {
    }

    @Override
    public CompileContext newCompileContext(Environment.Shared shared) {
        return new CompileContext(shared);
    }
}
