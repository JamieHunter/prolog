// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

import prolog.execution.Backtrack;
import prolog.execution.CompileContext;
import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.InstructionPointer;

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
    public void forward(InstructionPointer ip) {
        ip.next();
    }

    @Override
    public void invoke(Environment environment, Instruction inst) {
        inst.invoke(environment);
    }

    @Override
    public void backtrack(Backtrack bt) {
        bt.backtrack();
    }

    @Override
    public void reset() {
    }

    @Override
    public void decisionPoint(DecisionPoint decisionPoint) {
    }

    @Override
    public void pushIP(InstructionPointer ip) {
    }

    @Override
    public CompileContext newCompileContext(Environment.Shared shared) {
        return new CompileContext(shared);
    }
}
