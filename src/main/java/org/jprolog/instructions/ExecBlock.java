// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.execution.CompileContext;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.execution.InstructionIterator;
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
        environment.callIP(new BlockIterator(environment));
    }

    /**
     * Simple block iterator. Note that this iterator will pop the stack immediately BEFORE executing the last
     * (tail) instruction.
     */
    private class BlockIterator extends InstructionIterator {
        int iter = 0;

        BlockIterator(Environment environment) {
            super(environment);
        }

        /**
         * Progress forward through the block.
         */
        @Override
        public void next() {
            if (iter == block.length) {
                // we are at end of block except for tail
                // restore previous IP, no need to keep this
                // and continue execution at tail
                // This is a critical part of tail-call elimination, and assumes
                // recursion will typically occur at the tail.
                environment.restoreIP();
                tail.invoke(environment);
            } else {
                // iterating block
                block[iter++].invoke(environment);
            }
        }
    }
}