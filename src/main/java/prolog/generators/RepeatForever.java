// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.generators;

import prolog.execution.DecisionPointImpl;
import prolog.execution.Environment;

/**
 * Repeat forever (see "repeat" predicate).
 */
public class RepeatForever extends DecisionPointImpl {

    private RepeatForever(Environment environment) {
        super(environment);
    }

    /**
     * Create repeating loop.
     *
     * @param environment Environment to execute
     */
    public static void run(Environment environment) {
        new RepeatForever(environment).redo();
    }

    /**
     * Repeated choice points
     */
    @Override
    public final void redo() {
        environment.forward();
        environment.pushDecisionPoint(this);
    }
}
