// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.constants;

import org.jprolog.execution.Environment;
import org.jprolog.expressions.Term;
import org.jprolog.bootstrap.Interned;

/**
 * A self describing self referencing entity. In other languages, it would be considered a symbol or an enum value.
 * Equality is determined almost entirely by reference.
 */
public class PrologAtomInterned extends PrologAtomLike {
    private final String name;

    /**
     * Do not call this, use {@link Environment#internAtom(String)} instead.
     *
     * @param name Name of atom
     * @return New atom with given name
     */
    public static PrologAtomInterned internalNew(String name) {
        return new PrologAtomInterned(name);
    }

    /**
     * Made private as construction should be done indirectly either via {@link Interned#internAtom(String)} or via
     * {@link Environment#internAtom(String)}.
     *
     * @param name Name of Atom
     */
    private PrologAtomInterned(String name) {
        this.name = name;
    }

    /**
     * Name of the atom.
     *
     * @return Name
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologAtomInterned intern(Environment environment) {
        return this;
    }

    /**
     * Intern an atom, error if not an atom
     * @param environment Execution environment
     * @param term Term to convert
     * @return Interned atom
     */
    public static PrologAtomInterned from(Environment environment, Term term) {
        return PrologAtomLike.from(term).intern(environment);
    }

    /**
     * Intern an atom, error if not an atom
     * @param environmentShared Shared execution environment
     * @param term Term to convert
     * @return Interned atom
     */
    public static PrologAtomInterned from(Environment.Shared environmentShared, Term term) {
        return PrologAtomLike.from(term).intern(environmentShared);
    }
}
