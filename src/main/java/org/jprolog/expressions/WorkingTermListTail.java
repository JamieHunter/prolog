// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.expressions;

import org.jprolog.constants.PrologEmptyList;

import java.util.Collections;
import java.util.List;

/**
 * Represents a non-concrete tail. This can be any term, however list validation usually rejects a tail that is
 * not a variable, so for practical purposes this is a variable in the tail position.
 */
public class WorkingTermListTail implements WorkingTermList {
    private final Term tail;
    /*package*/ WorkingTermListTail(Term tail) {
        this.tail = tail;
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
        return tail == PrologEmptyList.EMPTY_LIST;
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
        return tail;
    }
}
