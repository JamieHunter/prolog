// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

import prolog.execution.Instruction;
import prolog.expressions.CompoundTerm;
import prolog.expressions.CompoundTermImpl;
import prolog.predicates.PredicateDefinition;
import prolog.predicates.Predication;

/**
 * Used for debugging to identify a predicate from an instruction
 */
public class InstructionLookup implements InstructionReflection {
    public final Instruction instruction;
    public final Predication.Interned predication;

    public InstructionLookup(Predication.Interned predication, Instruction instruction) {
        this.predication = predication;
        this.instruction = instruction;
    }

    @Override
    public CompoundTerm reflect() {
        return new CompoundTermImpl(predication.functor());
    }
}
