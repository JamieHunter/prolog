// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

import prolog.execution.Instruction;
import prolog.expressions.CompoundTerm;

/**
 * An instruction that can be reflected upon.
 */
public interface InstructionReflection {
    /**
     * @return representation of instruction
     */
    CompoundTerm reflect();
}
