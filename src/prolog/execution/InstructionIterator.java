// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

import prolog.debugging.InstructionReporter;

/**
 * Instruction iterator is an abstract instruction pointer that maintains location state.
 */
public abstract class InstructionIterator implements InstructionPointer, InstructionReporter, Cloneable {

    protected final Environment environment;

    /**
     * Create a new iterator.
     * @param environment Execution environment
     */
    protected InstructionIterator(Environment environment) {
        this.environment = environment;
    }

    /**
     * Make a clone copy of the location
     * @return copy
     */
    @Override
    public InstructionIterator copy() {
        try {
            return (InstructionIterator)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
