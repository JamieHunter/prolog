// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

import prolog.constants.PrologAtom;
import prolog.expressions.CompoundTerm;
import prolog.expressions.CompoundTermImpl;
import prolog.predicates.Predication;

import java.lang.ref.WeakReference;

/**
 * Provides a proxy to a discovered instruction.
 */
public class InstructionContext {

    private final CompoundTerm source;
    private final Predication.Interned predication;
    private final long id;
    private long specGen = 0;
    private int spyFlags = 0;

    public static final InstructionContext NULL = new InstructionContext(null, null, 0);

    public InstructionContext(Predication.Interned predication, DebugInstruction instruction, long id) {
        // Note, reference to instruction will kill the WeakHashMap.
        this.predication = predication;
        this.source = instruction == null ? null : instruction.getSource();
        this.id = id;
    }

    public CompoundTerm getSource() {
        return source;
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
