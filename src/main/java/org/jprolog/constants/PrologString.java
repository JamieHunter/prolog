// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.constants;

import org.jprolog.bootstrap.Interned;
import org.jprolog.exceptions.FutureTypeError;
import org.jprolog.expressions.Term;
import org.jprolog.expressions.TypeRank;
import org.jprolog.io.TermWriter;
import org.jprolog.io.WriteContext;

import java.io.IOException;

/**
 * A string in Prolog. Note that strings are not considered atoms but are considered constants. Also note that
 * while they have similar functions, "string" is not equivalent to `string`.
 */
public class PrologString extends AtomicBase {

    private final String value;

    /**
     * Create a Prolog String from a Java String
     *
     * @param value String value
     */
    public PrologString(String value) {
        if (value == null) {
            throw new NullPointerException("Creating null string");
        }
        this.value = value;
    }

    /**
     * Convert term to PrologString
     *
     * @param term Term to convert
     * @return converted term
     */
    public static PrologString from(Term term) {
        if (term instanceof PrologString) {
            return (PrologString) term;
        }
        throw new FutureTypeError(Interned.STRING_TYPE, term);
    }

    /**
     * Retrieve Java string value
     *
     * @return String value
     */
    @Override
    public String get() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isString() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        return obj instanceof PrologString && value.equals(((PrologString) obj).value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return value.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(WriteContext context) throws IOException {
        new TermWriter<PrologString>(context) {

            @Override
            public void write(Term term) throws IOException {
                writeMaybeQuoted('"', get());
            }
        }.write(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int typeRank() {
        return TypeRank.STRING;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareSameType(Term o) {
        // per standard, arity precedes functor in ordering
        PrologString other = (PrologString) o;
        return value.compareTo(other.value);
    }
}
