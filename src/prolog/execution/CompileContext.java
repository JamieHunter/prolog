// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

import prolog.instructions.ExecBlock;

import java.util.ArrayList;

/**
 * This context is used to compile an executable block of sequential instructions. The number of instructions compiled
 * is not 1:1 with the number of predicates in a comma sequence.
 */
public final class CompileContext {
    private final Environment environment;
    private final ArrayList<Instruction> instructions = new ArrayList<>();

    /**
     * Begin a new compile block associated with environment.
     *
     * @param environment Execution environment.
     */
    public CompileContext(Environment environment) {
        this.environment = environment;
    }

    /**
     * @return execution environment.
     */
    public Environment environment() {
        return environment;
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
     * Add an instruction to the block.
     *
     * @param instruction Instruction to add.
     */
    public void add(Instruction instruction) {
        if (instruction instanceof ExecBlock) {
            // flatten blocks
            instructions.addAll(((ExecBlock)instruction).all());
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
        return new CompileContext(environment);
    }
}
