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
 * Interface that allows a debugger to hook into debugger execution
 */
public interface DebuggerHook {
    void trace();

    void invoke(Environment environment, DebugInstruction context, Instruction instruction);

    void redo(DebugDecisionPoint context, DecisionPoint decisionPoint);

    DecisionPoint acceptDecisionPoint(DecisionPoint decisionPoint);

    void setExecution(ExecutionPoint ep, TransferHint hint);

    CompileContext newCompileContext(Environment.Shared shared);
}
