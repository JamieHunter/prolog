// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.unification.Unifier;
import org.jprolog.unification.UnifyBuilder;

/**
 * Unify two compound terms
 */
public class ExecUnifyCompounds implements Instruction {

    private final Unifier left;
    private final CompoundTerm right;

    /**
     * Create unify instruction.
     *
     * @param left   First term
     * @param right  Second term
     */
    public ExecUnifyCompounds(CompoundTerm left, CompoundTerm right) {
        this.left = UnifyBuilder.from(left); // precompile as unifier
        this.right = right;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invoke(Environment environment) {
        if (!left.unify(environment.getLocalContext(), right.resolve(environment.getLocalContext()))) {
            environment.backtrack();
        }
    }
}
