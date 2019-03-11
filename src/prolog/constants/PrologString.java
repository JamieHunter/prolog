// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.constants;

import prolog.io.TermWriter;
import prolog.io.WriteContext;

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
        this.value = value;
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
        new TermWriter<PrologString>(context, this) {

            @Override
            public void write() throws IOException {
                writeQuoted('"', term.get());
            }
        }.write();
    }
}
