// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.constants;

import prolog.bootstrap.Interned;
import prolog.exceptions.FutureTypeError;
import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.expressions.TypeRank;
import prolog.io.AtomWriter;
import prolog.io.WriteContext;

import java.io.IOException;

/**
 * An atom, but unlike a strict atom, this incorporates alternative encodings of an atom, and ability to compare between
 * them.
 */
public abstract class PrologAtomLike extends AtomicBase {

    /**
     * Limited scope constructor
     */
    protected PrologAtomLike() {
    }

    /**
     * @return true indicating this is an atom.
     */
    @Override
    public boolean isAtom() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return name();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get() {
        return name();
    }

    /**
     * Name of the atom.
     *
     * @return Name
     */
    abstract public String name();

    /**
     * Intern the atom in the context of environment for faster operations
     * @param environment Execution environment
     * @return atom
     */
    public PrologAtomInterned intern(Environment environment) {
        return environment.internAtom(name());
    }

    /**
     * When compiling, an atom is considered to be a predication of name/0 and handled via CompoundTerm compilation.
     *
     * @param compiling Context for compiling
     */
    @Override
    public void compile(CompileContext compiling) {
        CompoundTerm.from(intern(compiling.environment())).compile(compiling);
    }

    /**
     * Write quoted atom.
     *
     * @param context Write context
     * @throws IOException on IO error
     */
    @Override
    public void write(WriteContext context) throws IOException {
        new AtomWriter(context).write(this);
    }

    /**
     * Return true if this atom really needs to be quoted (used during parsing)
     * @return true if atom needs to be quoted
     */
    public boolean needsQuoting() {
        // reuse AtomWriter to make this determination
        return AtomWriter.needsQuoting(this);
    }

    /**
     * "Cast" a term to an atom
     *
     * @param term Term assumed to be an atom, or castable to an atom
     * @return term as an atom
     */
    public static PrologAtomLike from(Term term) {
        if (term.isAtom()) {
            return (PrologAtomLike) term;
        } else {
            throw new FutureTypeError(Interned.ATOM_TYPE, term);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int typeRank() {
        return TypeRank.ATOM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareSameType(Term o) {
        if (this == o) {
            return 0;
        }
        // generic comparison that can do relative comparison
        return name().compareTo(((PrologAtomLike) o).name());
    }

    /**
     * Note, equals/hashCode is not defined, as it is not possible to correctly define these and get
     * desired behavior. equals() therefore is for reference equality and only makes sense with interned atoms.
     */

}
