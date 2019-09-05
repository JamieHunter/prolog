// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.constants;

import prolog.expressions.TermList;
import prolog.expressions.Term;
import prolog.expressions.TypeRank;
import prolog.io.WriteContext;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * An empty list is a special constant. In some versions of Prolog, '[]' = [],
 * however others, '[]' != [], we're taking this latter approach to make empty list distinct.
 * Note that [] is not actually a {@link TermList}. A TermList is only
 * valid for lists containing at least one member element.
 */
public final class PrologEmptyList extends AtomicBase {

    public static final PrologEmptyList EMPTY_LIST = new PrologEmptyList();

    private PrologEmptyList() {
    }

    @Override
    public String toString() {
        return "[]";
    }

    @Override
    public List<Term> get() {
        return Collections.emptyList();
    }

    @Override
    public void write(WriteContext context) throws IOException {
        context.write("[]");
        context.beginSafe();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int typeRank() {
        return TypeRank.EMPTY_LIST; // between atom and compound
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareSameType(Term o) {
        return 0;
    }
}
