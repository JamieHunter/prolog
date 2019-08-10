// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.constants;

import prolog.exceptions.PrologDomainError;
import prolog.exceptions.PrologTypeError;
import prolog.execution.Environment;
import prolog.expressions.Container;
import prolog.expressions.Term;
import prolog.io.WriteContext;

import java.io.IOException;

/**
 * Represents a character in Prolog. It's seen by the application as an atom, but this avoids conversion to an atom
 * until necessary.
 */
public class PrologCharacter extends AtomicBase implements Container {
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
    public String toString() {
        return String.valueOf(value);
    }

    /**
     * Convert character to an atom if unifiable value is required.
     * @param environment Environment to retrieve/create atom
     * @return atom
     */
    @Override
    public Term value(Environment environment) {
        return environment.getAtom(String.valueOf(value));
    }

    /** {@inheritDoc} */
    @Override
    public void write(WriteContext context) throws IOException {
        value(context.environment()).write(context);
    }

    /**
     * Convert to character from atom or integer
     * @return Character
     */
    public static PrologCharacter from(Environment environment, Term source) {
        if (source instanceof PrologCharacter) {
            return (PrologCharacter)source;
        }
        if (source instanceof PrologAtom) {
            String t = ((PrologAtom)source).name();
            if (t.length() > 0) {
                return new PrologCharacter(t.charAt(0));
            }
        }
        if (source instanceof PrologInteger) {
            return new PrologCharacter((char)((PrologInteger)source).get().intValue());
        }
        throw PrologTypeError.characterExpected(environment, source);
    }
}
