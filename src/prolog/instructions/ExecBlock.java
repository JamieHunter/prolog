// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.InstructionIterator;
import prolog.expressions.Term;
import prolog.library.Control;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

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
     * Convert array of sequential instructions into a single instruction.
     *
     * @param environment Execution environment
     * @param term single term to compile
     * @return Single instruction including instruction block
     */
    public static Instruction from(Environment environment, Term term) {
        CompileContext context = new CompileContext(environment);
        term.compile(context);
        return context.toInstruction();
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
     * Retrieve all instructions as a collection.
     * @return collection of instructions
     */
    public Collection<? extends Instruction> all() {
        return Collections.unmodifiableList(Arrays.asList(block));
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
