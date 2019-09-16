// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.callstack;

import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * A spliterator to iterate the execution stack
 */
public class ExecutionSpliterator implements Spliterator<ResumableExecutionPoint> {

    private ResumableExecutionPoint current;

    public ExecutionSpliterator(ExecutionPoint top) {
        current = top.freeze();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean tryAdvance(Consumer<? super ResumableExecutionPoint> action) {
        if (current instanceof ExecutionTerminal) {
            return false;
        }
        ResumableExecutionPoint advance = current.previousExecution();
        action.accept(current);
        current = advance;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Spliterator<ResumableExecutionPoint> trySplit() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long estimateSize() {
        if (current instanceof ExecutionTerminal) {
            return 0;
        } else if (current.previousExecution() instanceof ExecutionTerminal) {
            return 1;
        } else {
            return Long.MAX_VALUE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getExactSizeIfKnown() {
        ExecutionPoint it = current;
        int count = 0;
        while(! (it instanceof ExecutionTerminal)) {
            count++;
            it = it.previousExecution();
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int characteristics() {
        return ORDERED | DISTINCT | NONNULL;
    }

}
