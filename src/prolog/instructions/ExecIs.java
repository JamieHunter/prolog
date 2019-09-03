// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.exceptions.PrologInstantiationError;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.functions.CompileMathExpression;
import prolog.unification.Unifier;

/**
 * Instantiate variable from data on stack. This is used to implement the final step of IS instruction.
 */
public class ExecIs implements Instruction {

    private final Instruction ops;
    private final Term target;

    /**
     * Create instruction that takes value of stack and unifies it with target.
     *
     * @param expr Math expression for RHS of Is
     * @param target Target term, assumed to be a variable.
     */
    public ExecIs(CompileMathExpression expr, Term target) {
        this.ops = expr.toInstruction();
        this.target = target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invoke(Environment environment) {
        // Execute math expression (not debuggable, known to be deterministic
        ops.invoke(environment);
        Term boundTarget = target.resolve(environment.getLocalContext());
        Term source = environment.pop(); // from stack
        if (!source.isInstantiated()) {
            throw PrologInstantiationError.error(environment, source);
        }
        if (!(boundTarget.instantiate(source) ||
                Unifier.unify(environment.getLocalContext(), boundTarget, source))) {
            environment.backtrack(); // left was instantiated, and values do not unify
        }
    }
}
