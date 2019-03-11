// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

/**
 * Base interface for all instructions. The interpreter executes invoke on the instruction at the current IP. See also
 * {@link InstructionPointer}.
 */
public interface Instruction {
    /**
     * Execute this instruction
     * @param environment Execution environment
     */
    void invoke(Environment environment);
}
