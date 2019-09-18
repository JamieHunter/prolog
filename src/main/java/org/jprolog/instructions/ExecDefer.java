// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.callstack.ImmutableExecutionPoint;
import org.jprolog.callstack.ResumableExecutionPoint;
import org.jprolog.callstack.TransferHint;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;

/**
 * Defers execution, forcing three states - push, exec, restore.
 */
public class ExecDefer implements Instruction {

    private final Instruction deferred;

    public ExecDefer(Instruction deferred) {
        this.deferred = deferred;
    }

    /**
     * Handle a call into the block. This effectively pushes an iterator IP
     *
     * @param environment Execution environment
     */
    public void invoke(Environment environment) {
        defer(environment, deferred);
    }

    /**
     * Handle a call into the block. This effectively pushes an iterator IP
     *
     * @param environment Execution environment
     * @param inst        Instruction to defer
     */
    public static void defer(Environment environment, Instruction inst) {
        environment.setExecution(new ExecDefer.Defer(environment, inst), TransferHint.CONTROL);
    }

    private static class Defer implements ImmutableExecutionPoint {
        private final Environment environment;
        private final Instruction deferred;
        private final ResumableExecutionPoint previous;

        Defer(Environment environment, Instruction deferred) {
            this.environment = environment;
            this.deferred = deferred;
            this.previous = environment.getExecution().freeze();
        }

        @Override
        public void invokeNext() {
            environment.setExecution(previous, TransferHint.LEAVE);
            deferred.invoke(environment);
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
