// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.expressions.Term;
import org.jprolog.unification.Unifier;
import org.jprolog.variables.Variable;

/**
 * Unify when one or both terms are variables (common simple unification)
 */
public class ExecUnifyInstantiate implements Instruction {

    private final Variable var;
    private final Term other;

    /**
     * Create unify instruction.
     *
     * @param var   First term, always a variable.
     * @param other Second term
     */
    public ExecUnifyInstantiate(Variable var, Term other) {
        this.var = var;
        this.other = other;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invoke(Environment environment) {
        Term leftBound = var.resolve(environment.getLocalContext()); // always a variable
        Term rightBound = other.resolve(environment.getLocalContext()); // sometimes a variable
        Unifier.unifyTerm(environment, leftBound, rightBound);
    }
}
