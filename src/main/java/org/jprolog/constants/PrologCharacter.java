// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.constants;

import org.jprolog.exceptions.PrologTypeError;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.Term;

/**
 * Represents a character in Prolog. It is considered an atom, but with deferred behavior.
 */
public class PrologCharacter extends PrologAtomLike {
    private final char value;

    public PrologCharacter(char value) {
        this.value = value;
    }

    /**
     * Retrieve the underlying character
     * @return character
     */
    @Override
    public Character get() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public String name() {
        return String.valueOf(value);
    }

    /**
     * Convert to character from atom or integer
     * @return Character
     */
    public static PrologCharacter from(Environment environment, Term source) {
        if (source instanceof PrologCharacter) {
            return (PrologCharacter)source;
        }
        if (source instanceof PrologAtomLike) {
            String t = ((PrologAtomLike)source).name();
            if (t.length() > 0) {
                return new PrologCharacter(t.charAt(0));
            }
        }
        if (source instanceof PrologInteger) {
            return new PrologCharacter(((PrologInteger)source).toChar());
        }
        throw PrologTypeError.characterExpected(environment, source);
    }
}
