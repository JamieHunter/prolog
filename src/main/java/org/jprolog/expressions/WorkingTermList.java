// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.expressions;

import java.util.List;

/**
 * This interface allows working with a term list and empty list for manipulation
 */
public interface WorkingTermList {

    /**
     * Retrieve head (first element) of list
     *
     * @return Head term
     */
    Term getAt(int index);

    /**
     * Alias of getAt(0).
     *
     * @return Head term
     */
    default Term getHead() {
        return getAt(0);
    }

    /**
     * Retrieve all but first
     *
     * @return Tail term
     */
    WorkingTermList getTailList();

    /**
     * Retrieve everything after concrete terms
     *
     * @return Tail term
     */
    WorkingTermList getFinalTailList();

    /**
     * @return Length of list contained in TermList, ignoring tail component
     */
    int concreteSize();

    /**
     * @return true if this is the empty list
     */
    boolean isEmptyList();

    /**
     * @return true if length describes a concrete length
     */
    boolean isConcrete();

    /**
     * @return as a Java list
     */
    List<Term> asList();

    /**
     * @return Apply tail n times.
     */
    WorkingTermList subList(int n);

    /**
     * @return list as a term
     */
    Term toTerm();
}
