// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.constants;

import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.expressions.TermList;

/**
 * Represents a list of characters (atoms of length 1).
 */
public class PrologChars extends PrologStringAsList {

    public PrologChars(CharSequence value) {
        super(value);
    }

    /**
     * Convert list to PrologChars
     * @param environment Execution environment
     * @param term Term to convert
     * @return converted term (empty-list remains unconverted).
     */
    public static Term from(Environment environment, Term term) {
        if (term == PrologEmptyList.EMPTY_LIST || term instanceof PrologChars) {
            return term;
        }
        String text = TermList.extractString(environment, term);
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
}
