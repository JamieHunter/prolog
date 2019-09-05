// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.execution.DecisionPointImpl;
import prolog.execution.Environment;
import prolog.execution.Instruction;

/**
 * Singleton repeat instruction. On backtrack, always succeeds.
 */
public final class ExecRepeat implements Instruction {

    /**
     * Singleton repeat instruction
     */
    public static final ExecRepeat REPEAT = new ExecRepeat();

    private ExecRepeat() {
    }

    /**
     * Repeat always succeed (except for a cut). Note that it is by design that
     * all variable instantiations are lost between repeats.
     */
    @Override
    public void invoke(Environment environment) {
        environment.pushDecisionPoint(new Repeat(environment));
    }

    /**
     * A decision point that always succeeds
     */
    private static class Repeat extends DecisionPointImpl {
        Repeat(Environment environment) {
            super(environment);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void redo() {
            environment.pushBacktrack(this);
            environment.forward();
        }
    }
}
