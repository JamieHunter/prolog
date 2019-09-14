// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.execution.CompileContext;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.expressions.Term;

/**
 * Defers compilation of a term to an instruction. When executed, it will compile a debuggable version or an
 * optimal version.
 */
public abstract class AbstractDeferredCompileInstruction implements Instruction {
    private volatile Instruction debuggable = null;
    private volatile Instruction optimal = null;

    /**
     * Called at start of compile process
     *
     * @return Body to compile
     */
    protected abstract Term begin();

    /**
     * Called at end of compile process
     *
     * @param context Compiling context
     * @return final instruction
     */
    protected abstract Instruction complete(CompileContext context);

    /**
     * Invoke instruction, compiling as necessary.
     *
     * @param environment Execution environment
     */
    @Override
    public void invoke(Environment environment) {
        select(environment).invoke(environment);
    }

    /**
     * Select correct compiled instruction for current running context, compiling
     * as necessary.
     *
     * @param environment Execution environment
     * @return Instruction to use
     */
    public Instruction select(Environment environment) {
        if (environment.isDebuggerEnabled()) {
            if (debuggable != null) {
                return debuggable;
            }
            CompileContext compileContext = environment.newCompileContext();
            begin().compile(compileContext);
            debuggable = complete(compileContext);
            return debuggable;
        } else {
            if (optimal != null) {
                return optimal;
            }
            CompileContext compileContext = environment.newCompileContext();
            begin().compile(compileContext);
            optimal = complete(compileContext);
            return optimal;
        }
    }
}
