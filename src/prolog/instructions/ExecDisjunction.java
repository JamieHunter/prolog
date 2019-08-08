// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.execution.Instruction;

/**
 * Disjunction explores different branches of code alternates.
 */
public class ExecDisjunction implements Instruction {

    private final Instruction[] alternates;

    /**
     * Build a disjunction from array of alternatives.
     *
     * @param alternates Array of alternatives Each is typically an {@link ExecBlock}.
     */
    public ExecDisjunction(Instruction[] alternates) {
        this.alternates = alternates;
    }

    /**
     * Invoke disjunction for the first time, creates a decision point.
     *
     * @param environment Execution environment
     */
    @Override
    public void invoke(Environment environment) {
        DisjunctionIterator iter = new DisjunctionIterator(environment);
        iter.next();
    }

    /**
     * Decision point for Disjunction.
     */
    private class DisjunctionIterator extends DecisionPoint {
        int iter = 0;

        DisjunctionIterator(Environment environment) {
            super(environment);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void next() {
            if (alternates.length != iter) {
                environment.forward();
                Instruction instr = alternates[iter++];
                if (iter != alternates.length) {
                    // insert decision point, and identify another solution exists
                    // this applies until there is a cut
                    environment.pushDecisionPoint(this);
                }
                instr.invoke(environment);
            }
        }
    }
}
