// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

/**
 * An instruction pointer is used to progress execution.
 */
public interface InstructionPointer {

    /**
     * Execute the next instruction at the instruction pointer, incrementing the IP by one location.
     */
    void next();

    /**
     * Make a copy of this instruction pointer (if necessary) that captures the current location. It is
     * a conditional clone.
     * @return Copy or self.
     */
    default InstructionPointer copy() {
        return this;
    }
}
