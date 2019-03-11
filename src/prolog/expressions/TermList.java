// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.expressions;

import prolog.constants.Atomic;
import prolog.constants.PrologEmptyList;
import prolog.execution.LocalContext;
import prolog.bootstrap.Interned;
import prolog.unification.HeadTailUnifyIterator;
import prolog.unification.UnifyIterator;

import java.util.ArrayList;

/**
 * Identifies that a compound term object is '.'(head,tail). Specifically this is used to represent a list, and
 * allows optimal storage and processing of lists constructed of these compound terms.
 */
public interface TermList extends CompoundTerm {

    /**
     * Return a unifier that is optimized towards lists.
     * @return Unify iterator
     */
    default UnifyIterator getUnifyIterator() {
        return new HeadTailUnifyIterator(this);
    }

    /**
     * Assume arity of 2
     * @return Arity
     */
    @Override
    default int arity() {
        return 2;
    }

    /**
     * Assume functor of list
     * @return Functor
     */
    @Override
    default Atomic functor() {
        return Interned.LIST_FUNCTOR;
    }

    /**
     * Resolve list
     * @param context Context for variable bindings
     * @return new list
     */
    @Override
    TermList resolve(LocalContext context);

    /**
     * Retrieve head (first element) of list
     * @return Head term
     */
    Term getHead();

    /**
     * Retrieve tail (remaining elements) of list
     * @return Tail term
     */
    Term getTail();

    /**
     * Get Head or Tail
     * @param i Argument index
     * @return Head (0) or Tail (1)
     */
    @Override
    default Term get(int i) {
        switch(i) {
            case 0:
                return getHead();
            case 1:
                return getTail();
            default:
                // Internal error not prolog error
                throw new IndexOutOfBoundsException("List node");
        }
    }

    /**
     * @return Length of list contained in TermList, ignoring tail component
     */
    int length();

    /**
     * Copy members to specified array, ignoring tail component
     * @param arr Target array
     */
    void copyMembers(ArrayList<Term> arr);

    /**
     * Assume [A,B,C,D|E] returns E. Very last tail component of list.
     * @return tail, or empty list
     */
    default Term lastTail() {
        return PrologEmptyList.EMPTY_LIST;
    }
}
