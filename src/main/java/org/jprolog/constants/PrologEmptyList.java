// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.constants;

import org.jprolog.expressions.Term;
import org.jprolog.expressions.TermList;
import org.jprolog.expressions.TypeRank;
import org.jprolog.expressions.WorkingTermList;
import org.jprolog.io.WriteContext;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * An empty list is a special constant. In some versions of Prolog, '[]' = [],
 * however others, '[]' != [], we're taking this latter approach to make empty list distinct.
 * Note that [] is not actually a {@link TermList}. A TermList is only
 * valid for lists containing at least one member element. For this reason, the interface WorkingTermList is introduced
 * that allows abstract term-list operations to be performed without having to implement a CompoundTerm.
 */
public final class PrologEmptyList extends AtomicBase implements WorkingTermList {

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

    /**
     * {@inheritDoc}
     */
    @Override
    public Term getAt(int index) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkingTermList getTailList() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkingTermList getFinalTailList() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int concreteSize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmptyList() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConcrete() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Term> asList() {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkingTermList subList(int n) {
        if (n != 0) {
            throw new IllegalArgumentException("Cannot create this sublist");
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term toTerm() {
        return this;
    }
}
