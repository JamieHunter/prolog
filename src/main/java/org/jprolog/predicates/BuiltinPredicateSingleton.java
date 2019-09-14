// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.predicates;

import org.jprolog.expressions.CompoundTerm;
import org.jprolog.execution.CompileContext;
import org.jprolog.execution.Instruction;

/**
 * A predicate that is built in to Prolog that compiles a singleton instruction.
 */
public class BuiltinPredicateSingleton extends BuiltInPredicate {
    private final Instruction instruction;
    public BuiltinPredicateSingleton(Instruction instruction) {
        this.instruction = instruction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void compile(Predication predication, CompileContext context, CompoundTerm term) {
        context.add(term, instruction);
    }
}
