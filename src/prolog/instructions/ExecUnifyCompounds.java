// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.expressions.CompoundTerm;
import prolog.unification.Unifier;
import prolog.unification.UnifyBuilder;

/**
 * Unify two compound terms
 */
public class ExecUnifyCompounds extends Traceable {

    private final Unifier left;
    private final CompoundTerm right;

    /**
     * Create unify instruction.
     *
     * @param source Source compound term
     * @param left  First term
     * @param right Second term
     */
    public ExecUnifyCompounds(CompoundTerm source, CompoundTerm left, CompoundTerm right) {
        super(source);
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
