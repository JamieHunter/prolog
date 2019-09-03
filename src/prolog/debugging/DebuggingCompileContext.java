// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

import prolog.bootstrap.Builtins;
import prolog.bootstrap.Interned;
import prolog.constants.PrologAtom;
import prolog.constants.PrologAtomInterned;
import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.expressions.CompoundTerm;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;
import prolog.instructions.ExecCall;
import prolog.predicates.ClauseEntry;
import prolog.predicates.Predication;

public class DebuggingCompileContext extends CompileContext {

    private static final CompoundTerm INTERNAL = new CompoundTermImpl(new PrologAtom("(internal)"));
    /**
     * Create a new compile block that compiles debugger-aware instructions
     *
     * @param environmentShared Execution shared environment.
     */
    public DebuggingCompileContext(Environment.Shared environmentShared) {
        super(environmentShared);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(CompoundTerm source, Instruction instruction) {
        if (instruction instanceof DebugInstruction) {
            super.add(null, instruction);
            return;
        }
        if (source == null) {
            source = INTERNAL;
        }
        instruction = new DebugInstruction(source, instruction, traceable(source));
        super.add(source, instruction);
    }

    /**
     * Add an instruction to the block, assumed to be / converted to callable.
     *
     * @param callable    Callable term
     * @param instruction Instruction to add.
     */
    @Override
    public void addCall(Term callable, ExecCall instruction) {
        CompoundTerm source = new CompoundTermImpl(Interned.CALL_FUNCTOR, callable);
        add(source, instruction);
    }

    /**
     * Convert a clause block to an instruction.
     *
     * @param entry Clause entry for this block
     * @return block instruction
     */
    @Override
    public ExecDebugClauseBlock toInstruction(ClauseEntry entry) {
        return ExecDebugClauseBlock.from(entry, instructions);
    }

    private Predication.Interned predication(CompoundTerm source) {
        if (source == null) {
            return null;
        }
        return new Predication(source.functor(), source.arity()).intern(environmentShared);
    }

    private SpySpec spec(CompoundTerm source) {
        if (source == null) {
            return null;
        }
        return SpySpec.from(PrologAtomInterned.from(environmentShared, source.functor()), source.arity());
    }

    private boolean traceable(CompoundTerm source) {
        if (source == INTERNAL) {
            return false;
        }
        SpySpec spec = spec(source);
        return !Builtins.isNoTrace(spec);
    }
}
