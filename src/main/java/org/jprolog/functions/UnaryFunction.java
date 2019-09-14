// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.functions;

import org.jprolog.expressions.Term;
import org.jprolog.constants.Atomic;
import org.jprolog.constants.PrologNumber;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;

import java.util.List;
import java.util.function.Function;

/**
 * A Mathematical unary function that delegates to lambda to execute.
 */
public class UnaryFunction implements StackFunction, Instruction {

    private final Function<PrologNumber, Atomic> lambda;

    /**
     * Construct a binary function instruction.
     *
     * @param lambda Actual binary function.
     */
    public UnaryFunction(Function<PrologNumber, Atomic> lambda) {
        this.lambda = lambda;
    }

    /**
     * Compile this function as an inline stack operation.
     *
     * @param compiling Compiling context
     */
    @Override
    public void compile(List<Instruction> compiling) {
        compiling.add(this);
    }

    /**
     * Invoke this function
     *
     * @param environment Execution environment
     */
    @Override
    public void invoke(Environment environment) {
        // Compiler has already enforced value on stack to be a number
        Term val = environment.pop();
        environment.push(lambda.apply((PrologNumber) val));
    }
}
