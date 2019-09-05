// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.InstructionIterator;
import prolog.library.Control;
import prolog.predicates.ClauseEntry;

import java.util.ArrayList;

/**
 * This is a replacement of ExecBlock that is used when a debug version is compiled
 */
public final class ExecDebugClauseBlock implements Instruction {

    private final ClauseEntry clauseEntry;
    private final Instruction[] block;
    private final Instruction tail;

    /**
     * Convert array of sequential instructions into a single instruction.
     *
     * @param instructions Array of instruction
     * @return Single instruction, typically a block
     */
    public static ExecDebugClauseBlock from(ClauseEntry clauseEntry, ArrayList<Instruction> instructions) {
        if (instructions.size() == 0) {
            instructions.add(Control.TRUE); // no-op
        }
        Instruction tail = instructions.remove(instructions.size() - 1);
        return new ExecDebugClauseBlock(clauseEntry, instructions, tail);
    }

    /**
     * Constructor to create an execution block.
     *
     * @param clauseEntry  Clause being executed
     * @param instructions Array of instructions excluding tail instruction
     */
    private ExecDebugClauseBlock(ClauseEntry clauseEntry, ArrayList<Instruction> instructions, Instruction tail) {
        this.clauseEntry = clauseEntry;
        this.block = instructions.toArray(new Instruction[instructions.size()]);
        this.tail = tail;
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
     * (tail) instruction if global flags indicate tail-call behavior.
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
            if (iter < block.length) {
                block[iter++].invoke(environment);
            } else if (iter == block.length) {
                // TODO: Logic to detect desired tail-call behavior and handle in debugger,
                // for now, take easier approach, and maintain stack
                iter++;
                tail.invoke(environment);
            } else {
                // return
                assert iter > 0;
                iter = -2; // not needed, but to aid debugging issues
                environment.restoreIP();
            }
        }
    }
}
