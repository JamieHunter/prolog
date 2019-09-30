// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.exceptions.PrologInstantiationError;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.expressions.Term;
import org.jprolog.functions.CompileMathExpression;
import org.jprolog.unification.Unifier;

/**
 * Instantiate variable from data on stack. This is used to implement the final step of IS instruction.
 */
public class ExecIs implements Instruction {

    private final Instruction ops;
    private final Term target;

    /**
     * Create instruction that takes value of stack and unifies it with target.
     *
     * @param expr   Math expression for RHS of Is
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
        Term boundTarget = target.resolve(environment.getLocalContext());
        // Execute math expression (not debuggable, known to be deterministic
        ops.invoke(environment);
        Term source = environment.pop(); // from stack
        if (!source.isInstantiated()) {
            throw PrologInstantiationError.error(environment, source);
        }
        // Inria tests suggest some ambiguity here, and that failure is expected rather than type error if boundTarget
        // is anything but number.
        Unifier.unifyTerm(environment, boundTarget, source);
    }
}
