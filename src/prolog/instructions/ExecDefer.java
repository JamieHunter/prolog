// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.InstructionIterator;

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
        environment.callIP(new ExecDefer.Defer(environment, deferred));
    }

    /**
     * States
     */
    private enum Iter {
        Start,
        Done
    }

    /**
     * Simple block iterator of one step.
     */
    private static class Defer extends InstructionIterator {

        private Iter iter = Iter.Start;
        private final Instruction deferred;

        Defer(Environment environment, Instruction deferred) {
            super(environment);
            this.deferred = deferred;
        }

        /**
         * Progress forward through the block.
         */
        @Override
        public void next() {
            switch (iter) {
                case Start:
                    iter = Iter.Done;
                    deferred.invoke(environment);
                    break;
                case Done:
                    environment.restoreIP();
                    break;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Instruction peek() {
            switch (iter) {
                case Start:
                    return deferred;
                default:
                    return null;
            }
        }
    }
}
