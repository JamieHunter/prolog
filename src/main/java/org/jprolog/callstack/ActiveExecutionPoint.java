// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.callstack;

/**
 * Execution Point is used for active execution. It may be an iterator, or may be a singleton.
 */
public interface ActiveExecutionPoint extends ExecutionPoint {

    /**
     * Invoke the next instruction, progressing any state.
     */
    void invokeNext();

    /**
     * {@inheritDoc}
     */
    default ActiveExecutionPoint activate() {
        return this;
    }

}
