// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.generators;

import org.jprolog.execution.DecisionPointImpl;
import org.jprolog.execution.Environment;

import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * This is converts a Java stream into Prolog solutions. The stream may be consumed one ahead of the yielded prolog
 * value.
 *
 * @param <T> Iterator type
 */
public class YieldSolutions<T> extends DecisionPointImpl {
    private final Spliterator<T> spliterator;
    private final Predicate<T> consumer;
    private T next = null;

    private YieldSolutions(Environment environment, Spliterator<T> spliterator, Predicate<T> consumer) {
        super(environment);
        this.spliterator = spliterator;
        this.consumer = consumer;
    }

    /**
     * Iterate over a set of values, yielding each value through process of backtracking. Note, consumer
     * may be called before previous value is yielded.
     *
     * @param environment Execution environment
     * @param stream      Stream of values
     * @param consumer    Consumer of next value, return true to yield the value, false to backtrack
     */
    public static <T> void forAll(Environment environment, Stream<T> stream, Predicate<T> consumer) {
        new YieldSolutions<T>(environment, stream.spliterator(), consumer).redo();
    }

    /**
     * Iterate over a set of values, yielding each value through process of backtracking. Note, consumer
     * may be called before previous value is yielded.
     *
     * @param environment Execution environment
     * @param spliterator Value spliterator
     * @param consumer    Consumer of next value, return true to yield the value, false to backtrack
     */
    public static <T> void forAll(Environment environment, Spliterator<T> spliterator, Predicate<T> consumer) {
        new YieldSolutions<T>(environment, spliterator, consumer).redo();
    }

    /**
     * Don't override/call this
     */
    @Override
    public final void redo() {
        if (next == null) {
            if (!spliterator.tryAdvance(n -> next = n)) {
                environment.backtrack();
                return;
            }
        }
        T value = next;
        if (spliterator.tryAdvance(n -> next = n)) {
            // there is a potential solution after this
            environment.pushDecisionPoint(this);
        } else {
            // there are no solutions after this
            next = null;
        }
        environment.forward();
        if (!consumer.test(value)) {
            environment.backtrack();
        }
    }
}
