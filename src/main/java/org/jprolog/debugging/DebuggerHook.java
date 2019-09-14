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
 * Interface that allows a debugger to hook into debugger execution
 */
public interface DebuggerHook {
    void trace();

    void invoke(Environment environment, DebugInstruction context, Instruction instruction);

    void redo(DebugDecisionPoint context, DecisionPoint decisionPoint);

    DecisionPoint acceptDecisionPoint(DecisionPoint decisionPoint);

    void acceptIP(InstructionPointer ip);

    void leaveIP(InstructionPointer ip);

    CompileContext newCompileContext(Environment.Shared shared);
}
