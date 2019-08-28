// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

import prolog.execution.Instruction;
import prolog.expressions.CompoundTerm;
import prolog.predicates.Predication;

/**
 * Expands beyond InstructionLookup providing dynamic context.
 */
public class InstructionContext extends InstructionLookup {

    private final long id;
    private final InstructionReflection reflection;
    private long specGen = 0;
    private int spyFlags = 0;

    public static final InstructionContext NULL = new InstructionContext(null, null, null, 0);

    public InstructionContext(Predication.Interned predication, Instruction instruction, InstructionReflection reflection, long id) {
        super(predication, instruction);
        this.id = id;
        this.reflection = reflection;
    }

    @Override
    public CompoundTerm reflect() {
        return reflection.reflect();
    }

    public InstructionContext(InstructionLookup lookup, long id) {
        super(lookup.predication, lookup.instruction);
        this.id = id;
        this.reflection = lookup;
    }

    public int spyFlags(SpyPoints spyPoints) {
        if (spyPoints.generation != this.specGen) {
            this.spyFlags = spyPoints.computeSpyFlags(predication);
            this.specGen = spyPoints.generation;
        }
        return spyFlags;
    }

    public long getId() {
        return id;
    }

    public SpySpec spySpec() {
        return SpySpec.from(predication);
    }
}
