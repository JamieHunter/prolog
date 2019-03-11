// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.bootstrap.Interned;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.expressions.Term;

/**
 * Singleton, pop value and test against 'true'. Used to implement a compare test.
 */
public class ExecPopAndTest implements Instruction {

    public static final ExecPopAndTest INSTRUCTION = new ExecPopAndTest();

    private ExecPopAndTest() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invoke(Environment environment) {
        Term value = environment.pop();
        if (value != Interned.TRUE_ATOM) {
            environment.backtrack();
        }
    }
}
