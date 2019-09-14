// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.bootstrap.Interned;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.expressions.Term;
import org.jprolog.functions.CompileMathExpression;

/**
 * Singleton, pop value and test against 'true'. Used to implement a compare test.
 */
public class ExecPopAndTest implements Instruction {
    private final Instruction ops;

    public ExecPopAndTest(CompileMathExpression expr) {
        this.ops = expr.toInstruction();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invoke(Environment environment) {
        // Execute math expression (not debuggable, known to be deterministic
        ops.invoke(environment);
        Term value = environment.pop();
        if (value != Interned.TRUE_ATOM) {
            environment.backtrack();
        }
    }
}
