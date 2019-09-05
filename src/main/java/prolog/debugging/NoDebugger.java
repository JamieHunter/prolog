// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

import prolog.execution.Backtrack;
import prolog.execution.CompileContext;
import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.InstructionIterator;
import prolog.execution.InstructionPointer;
import prolog.predicates.ClauseEntry;

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
