// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

/**
 * Instruction iterator is an abstract instruction pointer that maintains location state.
 */
public abstract class InstructionIterator implements InstructionPointer, Cloneable {

    protected final Environment environment;
    protected final Object ref = this; // original iterator

    /**
     * Create a new iterator.
     *
     * @param environment Execution environment
     */
    protected InstructionIterator(Environment environment) {
        this.environment = environment;
    }

    /**
     * Used within hash-maps to handle clones
     * @return reference to original iterator not intended to be used as an iterator)
     */
    @Override
    public Object ref() {
        return ref;
    }

    /**
     * Make a clone copy of the location
     *
     * @return copy
     */
    @Override
    public InstructionIterator copy() {
        try {
            return (InstructionIterator) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
