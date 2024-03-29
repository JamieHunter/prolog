// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.execution;

/**
 * Base interface for all instructions.
 */
public interface Instruction {
    /**
     * Execute this instruction
     *
     * @param environment Execution environment
     */
    void invoke(Environment environment);
}
