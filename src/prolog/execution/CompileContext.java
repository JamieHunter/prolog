// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

import prolog.instructions.ExecBlock;
import prolog.predicates.ClauseEntry;

import java.util.ArrayList;

/**
 * This context is used to compile an executable block of sequential instructions. The number of instructions compiled
 * is not 1:1 with the number of predicates in a comma sequence.
 */
public class CompileContext {
    private final Environment.Shared environmentShared;
    private final ArrayList<Instruction> instructions = new ArrayList<>();

    /**
     * Begin a new compile block associated with shared environment.
     *
     * @param environmentShared Execution shared environment.
     */
    public CompileContext(Environment.Shared environmentShared) {
        this.environmentShared = environmentShared;
    }

    /**
     * @return execution shared environment.
     */
    public Environment.Shared environmentShared() {
        return environmentShared;
    }

    /**
     * Convert the block to a single instruction. The single instruction is potentially TRUE (no-op), or a
     * single instruction if the block compiled to only one instruction, or a callable block of instructions.
     *
     * @return Single instruction.
     */
    public Instruction toInstruction() {
        return ExecBlock.from(instructions);
    }

    /**
     * Variation called when compiling a {@link ClauseEntry} to allow additional debug
     * information to be compiled.
     *
     * @return Single instruction or block.
     */
    public Instruction toInstruction(ClauseEntry entry) {
        return toInstruction();
    }

    /**
     * Add an instruction to the block.
     *
     * @param instruction Instruction to add.
     */
    public void add(Instruction instruction) {
        if (instruction instanceof ExecBlock) {
            // flatten blocks
            instructions.addAll(((ExecBlock) instruction).all());
        } else {
            instructions.add(instruction);
        }
    }

    /**
     * Create a new compile context associated with this compile context.
     *
     * @return New compile context.
     */
    public CompileContext newContext() {
        return new CompileContext(environmentShared);
    }
}
