// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.expressions.Term;
import prolog.variables.Variable;

/**
 * Put a variable that needs to be bound onto the stack. Base class for similar instructions that implement a check
 * method.
 */
public class ExecPushVariable implements Instruction {

    private final Variable var;

    /**
     * Create instruction with variable
     *
     * @param var Variable
     */
    public ExecPushVariable(Variable var) {
        this.var = var;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invoke(Environment environment) {
        Term t = var.resolve(environment.getLocalContext());
        check(environment, t);
        environment.push(t);
    }

    /**
     * Override to add tests to value.
     *
     * @param environment Execution environment
     * @param test        Value to test
     */
    void check(Environment environment, Term test) {
        // does nothing, override to perform a check
    }
}
