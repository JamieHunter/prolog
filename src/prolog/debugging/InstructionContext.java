// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

import prolog.expressions.CompoundTerm;
import prolog.predicates.Predication;

/**
 * Expands beyond InstructionLookup providing dynamic context.
 */
public class InstructionContext {

    private final DebugInstruction instruction;
    private final Predication.Interned predication;
    private final long id;
    private long specGen = 0;
    private int spyFlags = 0;

    public static final InstructionContext NULL = new InstructionContext(null, null, 0);

    public InstructionContext(Predication.Interned predication, DebugInstruction instruction, long id) {
        this.predication = predication;
        this.instruction = instruction;
        this.id = id;
    }

    public CompoundTerm getSource() {
        return instruction.getSource();
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
