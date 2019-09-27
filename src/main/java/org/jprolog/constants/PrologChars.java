// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.constants;

import org.jprolog.execution.Environment;
import org.jprolog.expressions.Strings;
import org.jprolog.expressions.Term;
import org.jprolog.expressions.TermList;
import org.jprolog.io.StructureWriter;
import org.jprolog.io.TermWriter;
import org.jprolog.io.WriteContext;

import java.io.IOException;

/**
 * Represents a list of characters (atoms of length 1).
 */
public class PrologChars extends PrologStringAsList {

    public PrologChars(CharSequence value) {
        super(value);
    }

    /**
     * Convert list to PrologChars
     * @param term Term to convert
     * @return converted term (empty-list remains unconverted).
     */
    public static Term from(Term term) {
        if (term == PrologEmptyList.EMPTY_LIST || term instanceof PrologChars) {
            return term;
        }
        String text = Strings.stringFromChars(term);
        if (text.isEmpty()) {
            return PrologEmptyList.EMPTY_LIST;
        } else {
            return new PrologChars(text);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Term toTerm(char c) {
        return new PrologCharacter(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PrologStringAsList substring(CharSequence value) {
        return new PrologChars(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(WriteContext context) throws IOException {
        new TermWriter<PrologStringAsList>(context) {
            @Override
            public void write(Term term) throws IOException {
                if (context.options().ignoreOps) {
                    new StructureWriter(context).write(term);
                } else {
                    writeMaybeQuoted('`', term.toString());
                }
            }
        }.write(this);
    }
}
