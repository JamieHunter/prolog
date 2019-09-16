// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.callstack;

/**
 * Common functionality between an ActiveExecutionPoint and a ResumableExecutionPoint
 */
public interface ExecutionPoint {

    /**
     * Used to identify an instance of execution. When execution transitions between active and resumable, it
     * maintains the same identifier. Id is represented as an opaque object.
     * @return id object
     */
    Object id();

    /**
     * Call to activate this execution.
     * @return active execution
     */
    ActiveExecutionPoint activate();

    /**
     * Call to freeze/snapshot an active execution
     * @return resumable execution
     */
    ResumableExecutionPoint freeze();

    /**
     * Previous in the stack of execution.
     * @return Previous
     */
    ResumableExecutionPoint previousExecution();
}
