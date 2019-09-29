// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.constants;

import org.jprolog.expressions.Strings;
import org.jprolog.expressions.Term;

/**
 * Represents a list of code points (integers).
 */
public class PrologCodePoints extends PrologStringAsList {

    public PrologCodePoints(CharSequence value) {
        super(value);
    }

    /**
     * Convert list to PrologCodePoints
     *
     * @param term Term to convert
     * @return converted term (empty-list remains unconverted).
     */
    public static Term from(Term term) {
        if (term == PrologEmptyList.EMPTY_LIST || term instanceof PrologCodePoints) {
            return term;
        }
        String text = Strings.stringFromCodePoints(term);
        if (text.isEmpty()) {
            return PrologEmptyList.EMPTY_LIST;
        } else {
            return new PrologCodePoints(text);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Term toTerm(char c) {
        return new PrologInteger((int) c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PrologStringAsList substring(CharSequence value) {
        return new PrologCodePoints(value);
    }
}
