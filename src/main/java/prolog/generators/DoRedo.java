// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.generators;

import prolog.execution.DecisionPointImpl;
import prolog.execution.Environment;

/**
 * This wraps before and after a redo.
 */
public class DoRedo extends DecisionPointImpl {
    private final Runnable first;
    private final Runnable second;

    private DoRedo(Environment environment, Runnable first, Runnable second) {
        super(environment);
        this.first = first;
        this.second = second;
    }

    /**
     * One iteration with redo
     *
     * @param environment Execution environment
     * @param first       Produce first solution (return true on success).
     * @param second      Produce second solution
     */
    public static void invoke(Environment environment, Runnable first, Runnable second) {
        new DoRedo(environment, first, second).run();
    }

    private void run() {
        environment.pushDecisionPoint(this);
        first.run();
    }

    /**
     * Don't override/call this
     */
    @Override
    public final void redo() {
        environment.forward();
        second.run();
    }
}
