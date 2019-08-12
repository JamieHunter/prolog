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
 * A self describing self referencing entity. In other languages, it would be considered a symbol or an enum value.
 * Equality is determined almost entirely by reference.
 */
public class PrologAtom extends AtomicBase {
    private final String name;

    /**
     * Do not call this, use {@link Environment#getAtom(String)} instead.
     *
     * @param name Name of atom
     * @return New atom with given name
     */
    public static PrologAtom internalNew(String name) {
        return new PrologAtom(name);
    }

    /**
     * Made private as construction should be done indirectly either via {@link Interned#internAtom(String)} or via
     * {@link Environment#getAtom(String)}.
     *
     * @param name Name of Atom
     */
    private PrologAtom(String name) {
        this.name = name;
    }

    /**
     * @return true indicating an atom is an atom.
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
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String get() {
        return name;
    }

    /**
     * Name of the atom.
     *
     * @return Name
     */
    public String name() {
        return name;
    }

    /**
     * When compiling, an atom is considered to be a predication of name/0 and handled via CompoundTerm compilation.
     *
     * @param compiling Context for compiling
     */
    @Override
    public void compile(CompileContext compiling) {
        CompoundTerm.from(this).compile(compiling);
    }

    /**
     * Write quoted atom.
     *
     * @param context Write context
     * @throws IOException on IO error
     */
    @Override
    public void write(WriteContext context) throws IOException {
        new AtomWriter(context, this).write();
    }

    /**
     * "Cast" a term to an atom
     *
     * @param term Term assumed to be an atom, or castable to an atom
     * @return term as an atom
     */
    public static PrologAtom from(Term term) {
        if (term.isAtom()) {
            return (PrologAtom) term;
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
        return name().compareTo(((PrologAtom) o).name());
    }

}
