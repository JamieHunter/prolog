package prolog.instructions;

import prolog.debugging.InstructionReflection;
import prolog.execution.Instruction;
import prolog.expressions.CompoundTerm;

/**
 * An instruction that also has a reference to source
 */
public abstract class Traceable implements InstructionReflection, Instruction {

    private final CompoundTerm source;

    public Traceable(CompoundTerm source) {
        this.source = source;
    }

    /**
     * {@inheritDoc}
     */
    public CompoundTerm reflect() {
        return source;
    }
}
