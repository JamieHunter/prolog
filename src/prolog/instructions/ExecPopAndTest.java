// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.bootstrap.Interned;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.functions.CompileMathExpression;

/**
 * Singleton, pop value and test against 'true'. Used to implement a compare test.
 */
public class ExecPopAndTest extends Traceable {
    private final Instruction ops;

    public ExecPopAndTest(CompoundTerm source, CompileMathExpression expr) {
        super(source);
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
