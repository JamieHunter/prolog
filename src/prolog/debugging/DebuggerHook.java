// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

import prolog.execution.Backtrack;
import prolog.execution.CompileContext;
import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.InstructionPointer; /**
 * Interface that allows a debugger to hook into debugger execution
 */
public interface DebuggerHook {
    void trace();

    void forward(InstructionPointer ip);

    void invoke(Environment environment, Instruction inst);

    void backtrack(Backtrack bt);

    void reset();

    void decisionPoint(DecisionPoint decisionPoint);

    void pushIP(InstructionPointer ip);

    CompileContext newCompileContext(Environment.Shared shared);
}
