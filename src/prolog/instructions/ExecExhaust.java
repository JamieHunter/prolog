// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.debugging.InstructionReflection;
import prolog.execution.CompileContext;
import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.InstructionPointer;

import java.util.function.Consumer;

/**
 * Repeatedly call an instruction until that instruction fails. This instruction succeeds if there was at least one
 * solution.
 */
public final class ExecExhaust implements Instruction {
    private final Instruction precompiled;

    /**
     * Create instruction
     *
     * @param compiling Compiling context
     * @param comp        Given a nested CompileContext, provide an instruction.
     */
    public ExecExhaust(CompileContext compiling, Consumer<CompileContext> comp) {
        CompileContext child = compiling.newContext();
        comp.accept(child);
        this.precompiled = child.toInstruction();
    }

    /**
     * Begin loop
     *
     * @param environment Execution environment
     */
    @Override
    public void invoke(Environment environment) {
        RepeatLoop loop = new RepeatLoop(environment, precompiled);
        // Execute code
        loop.begin();
    }

    /**
     * Decision point on first round executes instruction. On second round, determines if the repetition should be
     * considered successful or not.
     */
    private static class RepeatLoop extends DecisionPoint {

        final Instruction block;
        boolean success = false; // set to true on success
        final LoopRepeat repeat;

        RepeatLoop(Environment environment, Instruction precompiled) {
            super(environment);
            this.block = precompiled;
            this.repeat = new LoopRepeat(this);
        }

        /**
         * Begin first iteration.
         */
        void begin() {
            environment.pushDecisionPoint(this);
            environment.callIP(repeat);
            block.invoke(environment);
        }

        /**
         * Begin second iteration, succeeds if first iteration reached {@link LoopRepeat}.
         */
        @Override
        protected void next() {
            // we only  get here if all attempts are exhausted
            if (success) {
                // succeeded at least once
                environment.forward();
            } else {
                // did not succeed at all
                environment.backtrack();
            }
        }

        /**
         * Called if first iteration succeeded completely. Cause backtracking. Backtracking will keep entering
         * the instruction (block) until all solutions are exhausted, and then will backtrack into {@link RepeatLoop}.
         */
        void repeat() {
            success = true;
            environment.backtrack();
        }
    }

    /**
     * Other end of repeat, this will cause each successful iteration to backtrack
     */
    protected static class LoopRepeat implements InstructionPointer {
        final RepeatLoop loop;

        LoopRepeat(RepeatLoop loop) {
            this.loop = loop;
        }

        @Override
        public void next() {
            loop.repeat();
        }

        @Override
        public InstructionPointer copy() {
            return this;
        }
    }
}
