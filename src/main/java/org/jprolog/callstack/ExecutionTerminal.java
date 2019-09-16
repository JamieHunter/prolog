// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.callstack;

public class ExecutionTerminal implements ImmutableExecutionPoint {

    private final Runnable terminal;

    public ExecutionTerminal(Runnable terminal) {
        this.terminal = terminal;
    }

    @Override
    public void invokeNext() {
        terminal.run();
    }

    @Override
    public Object id() {
        return this;
    }

    @Override
    public ResumableExecutionPoint previousExecution() {
        return null;
    }
}
