// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.functions;

import prolog.constants.Atomic;
import prolog.constants.PrologNumber;
import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.expressions.Term;

import java.util.function.BiFunction;

/**
 * Mathematical binary function that delegates to lambda to execute.
 */
public class BinaryFunction implements StackFunction, Instruction {

    private final BiFunction<PrologNumber, PrologNumber, Atomic> lambda;

    /**
     * Construct a binary function instruction.
     *
     * @param lambda Actual binary function.
     */
    public BinaryFunction(BiFunction<PrologNumber, PrologNumber, Atomic> lambda) {
        this.lambda = lambda;
    }

    /**
     * Compile this function as an inline stack operation.
     *
     * @param compiling Compiling context
     */
    @Override
    public void compile(CompileContext compiling) {
        compiling.add(this);
    }

    /**
     * Invoke this function
     *
     * @param environment Execution environment
     */
    @Override
    public void invoke(Environment environment) {
        // Compiler has already enforced top items of stack to be numbers
        Term right = environment.pop();
        Term left = environment.pop();
        environment.push(lambda.apply((PrologNumber) left, (PrologNumber) right));
    }
}
