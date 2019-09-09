// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.exceptions.PrologInstantiationError;
import prolog.exceptions.PrologTypeError;
import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.expressions.Term;
import prolog.functions.CompileMathExpression;
import prolog.variables.Variable;

/**
 * Put a value that needs to be bound and resolved during runtime onto the stack. Value is required to be a number.
 */
public class ExecEvaluateVariable implements Instruction {

    private final Variable var;

    /**
     * {@inheritDoc}
     */
    public ExecEvaluateVariable(Variable var) {
        this.var = var;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invoke(Environment environment) {
        Term term = var.resolve(environment.getLocalContext());
        if (term.isNumber()) {
            // fast track
            environment.push(term);
            return;
        }
        if (!term.isInstantiated()) {
            throw PrologInstantiationError.error(environment, term);
        }
        // If not a number, treat as a sub-expression
        CompileContext context = environment.newCompileContext();
        CompileMathExpression expr = new CompileMathExpression(context).compile(term);
        expr.toInstruction().invoke(environment);
    }

    /**
     * Recursively evaluate expression provided as a variable.
     *
     * @param environment Execution environment
     * @param term        Term to check
     */
    protected Term parse(Environment environment, Term term) {
        if (term.isInstantiated()) {
            if (!term.isNumber()) {

                throw PrologTypeError.numberExpected(environment, term);
            }
        } else {
            throw PrologInstantiationError.error(environment, term);
        }
        return term;
    }
}
