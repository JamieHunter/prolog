// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.callstack.ActiveExecutionPoint;
import org.jprolog.callstack.ResumableExecutionPoint;
import org.jprolog.callstack.TransferHint;
import org.jprolog.execution.CompileContext;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.expressions.Term;
import org.jprolog.library.Control;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A block of instructions (conjunction). Each instruction is executed in turn as long as the executionState is forward.
 * If the executionState changes to backtracking, the backtracking stack is used instead.
 * Execution optimizes for tail-call elimination.
 */
public final class ExecBlock implements Instruction {

    private final Instruction[] block;
    private final Instruction tail;

    /**
     * Convert array of sequential instructions into a single instruction.
     *
     * @param instructions Array of instruction
     * @return Single instruction, typically a block
     */
    public static Instruction from(ArrayList<Instruction> instructions) {
        if (instructions.size() == 0) {
            return Control.TRUE;
        } else if (instructions.size() == 1) {
            return instructions.get(0);
        } else {
            Instruction tail = instructions.remove(instructions.size() - 1);
            return new ExecBlock(instructions, tail);
        }
    }

    /**
     * Retrieve a version without tail-call elimination
     * @param instructions Block of instructions
     * @return Complete block
     */
    public static ExecBlock debuggable(ArrayList<Instruction> instructions) {
        return new ExecBlock(instructions, Control.TRUE);
    }

    /**
     * Compile an independently nested block
     *
     * @param compiling Compiling context of calling block
     * @param term      Term to compile
     * @return Instruction
     */
    public static Instruction nested(CompileContext compiling, Term term) {
        CompileContext context = compiling.newContext();
        term.compile(context);
        return context.toInstruction();
    }

    /**
     * Deferred compile of a nested block (per call).
     *
     * @param term Term to call, compilation deferred
     * @return Instruction
     */
    public static Instruction deferred(Term term) {
        return new DeferredCallInstruction(term);
    }

    /**
     * Constructor to create an execution block.
     *
     * @param instructions Array of instructions excluding tail instruction
     * @param tail         Tail instruction - last instruction of block
     */
    private ExecBlock(ArrayList<Instruction> instructions, Instruction tail) {
        this.block = instructions.toArray(new Instruction[instructions.size()]);
        this.tail = tail;
    }

    /**
     * Utility to allow merging of blocks.
     *
     * @param instructions Instructions to append this block to.
     */
    public void mergeTo(ArrayList<Instruction> instructions) {
        instructions.addAll(Arrays.asList(block));
        instructions.add(tail);
    }

    /**
     * Handle a call into the block. This effectively pushes an iterator IP
     *
     * @param environment Execution environment
     */
    public void invoke(Environment environment) {
        environment.setExecution(new Instance(environment, block, tail).start(), TransferHint.ENTER);
    }

    /**
     * Instance defines this instance of a block (in response to an invoke). freeze/active will return two
     * sub-objects depending on state.
     */
    private static class Instance {
        final Environment environment;
        final Instruction [] block;
        final Instruction tail;
        final ResumableExecutionPoint previous;

        Instance(Environment environment, Instruction [] block, Instruction tail) {
            this.environment = environment;
            this.block = block;
            this.tail = tail;
            this.previous = environment.getExecution().freeze();
        }

        ActiveExecutionPoint start() {
            return new Active(0);
        }

        public class Frozen implements ResumableExecutionPoint {

            final int iter;

            Frozen(int iter) {
                this.iter = iter;
            }

            @Override
            public Object id() {
                return Instance.this;
            }

            @Override
            public ActiveExecutionPoint activate() {
                return new Active(iter);
            }

            @Override
            public ResumableExecutionPoint previousExecution() {
                return previous;
            }
        }

        public class Active implements ActiveExecutionPoint {

            int iter;

            Active(int iter) {
                this.iter = iter;
            }

            @Override
            public Object id() {
                return Instance.this;
            }

            @Override
            public ResumableExecutionPoint freeze() {
                return new Frozen(iter);
            }

            @Override
            public ResumableExecutionPoint previousExecution() {
                return previous;
            }

            @Override
            public void invokeNext() {
                if (iter == block.length) {
                    // we are at end of block except for tail
                    // restore previous execution, no need to keep this
                    // and continue execution at tail
                    // This is a critical part of tail-call elimination, and assumes
                    // recursion will typically occur at the tail.
                    environment.setExecution(previous, TransferHint.LEAVE);
                    tail.invoke(environment);
                } else {
                    // iterating block
                    block[iter++].invoke(environment);
                }
            }
        }
    }
}
