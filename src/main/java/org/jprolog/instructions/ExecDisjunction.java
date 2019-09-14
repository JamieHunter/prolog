// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.generators.YieldSolutions;

import java.util.Arrays;

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
        YieldSolutions.forAll(environment, Arrays.asList(alternates).stream(), i -> {
            i.invoke(environment);
            return true;
        });
    }
}
