// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.constants;

/**
 * A string that behaves as an atom
 */
public class PrologAtom extends PrologAtomLike {
    private final String name;

    public PrologAtom(String name) {
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
}
