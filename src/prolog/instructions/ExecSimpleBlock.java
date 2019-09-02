// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.library.Control;

import java.util.ArrayList;

/**
 * A block of instructions that are assumed to be, as a whole, atomic, to evaluate an expression. All instructions
 * can be evaluated within a single invoke.
 */
public final class ExecSimpleBlock implements Instruction {

    private final Instruction[] block;

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
            return new ExecSimpleBlock(instructions);
        }
    }

    private ExecSimpleBlock(ArrayList<Instruction> instructions) {
        block = instructions.toArray(new Instruction[instructions.size()]);
    }

    /**
     * Handle a call into the block. All child instructions occur before return.
     * The block is not expected to backtrack, and if an error occurs, the block
     * is expected to throw an exception.
     *
     * @param environment Execution environment
     */
    public void invoke(Environment environment) {
        for (int i = 0; i < block.length; i++) {
            block[i].invoke(environment);
        }
        if (!environment.isForward()) {
            throw new InternalError("Simple Block should never backtrack");
        }
    }
}
