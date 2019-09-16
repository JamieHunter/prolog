// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.callstack;

/**
 * A reference to execution that is presumed to not be directly executable without calling activate on it.
 */
public interface ResumableExecutionPoint extends ExecutionPoint {

    /**
     * {@inheritDoc}
     */
    @Override
    default ResumableExecutionPoint freeze() {
        return this;
    }
}
