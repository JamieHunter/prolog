// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

import prolog.execution.Instruction;

/**
 * If Implemented by an InstructionPointer, can report the instruction being pointed at
 */
public interface InstructionReporter {
    Instruction peek();
}
