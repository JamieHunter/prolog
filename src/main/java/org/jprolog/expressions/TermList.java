// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.expressions;

import org.jprolog.unification.HeadTailUnifyIterator;
import org.jprolog.unification.UnifyIterator;
import org.jprolog.bootstrap.Interned;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.constants.PrologEmptyList;
import org.jprolog.exceptions.FutureInstantiationError;
import org.jprolog.exceptions.FutureTypeError;
import org.jprolog.enumerators.EnumTermStrategy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Identifies that a compound term object is '.'(head,tail). Specifically this is used to represent a list, and
 * allows optimal storage and processing of lists constructed of these compound terms.
 */
public interface TermList extends CompoundTerm {

    /**
     * Return a unifier that is optimized towards lists.
     *
     * @return Unify iterator
     */
    default UnifyIterator getUnifyIterator() {
        return new HeadTailUnifyIterator(this);
    }

    /**
     * Assume arity of 2
     *
     * @return Arity
     */
    @Override
    default int arity() {
        return 2;
    }

    /**
     * Assume functor of list
     *
     * @return Functor
     */
    @Override
    default PrologAtomLike functor() {
        return Interned.LIST_FUNCTOR;
    }

    /**
     * Retrieve head (first element) of list
     *
     * @return Head term
     */
    Term getHead();

    /**
     * Retrieve tail (remaining elements) of list
     *
     * @return Tail term
     */
    Term getTail();

    /**
     * Get Head or Tail
     *
     * @param i Argument index
     * @return Head (0) or Tail (1)
     */
    @Override
    default Term get(int i) {
        switch (i) {
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
     * Copy members to specified array, ignoring tail component
     *
     * @param arr Target array
     */
    void copyMembers(ArrayList<Term> arr);

    /**
     * Assume [A,B,C,D|E] returns E. Very last tail component of list.
     *
     * @return tail, or empty list
     */
    default Term lastTail() {
        return PrologEmptyList.EMPTY_LIST;
    }

    Term enumTerm(EnumTermStrategy strategy);

    /**
     * True if term is a list
     *
     * @param term Term to test
     * @return true if list of 0 or more terms
     */
    static boolean isList(Term term) {
        return term == PrologEmptyList.EMPTY_LIST || CompoundTerm.termIsA(term, Interned.LIST_FUNCTOR, 2);
    }

    /**
     * Iterator to iterate through a list however constructed. Iterator will also throw exception if list is invalid.
     *
     * @param term Term to iterate
     * @return iterator
     */
    static TermIterator listIterator(Term term) {
        return new TermIterator(term);
    }

    class TermIterator implements Iterator<Term> {

        private Term origList;
        private Term next;

        public TermIterator(Term term) {
            this.origList = this.next = term;
        }

        @Override
        public boolean hasNext() {
            if (next == PrologEmptyList.EMPTY_LIST) {
                return false;
            } else if (isList(next)) {
                return true;
            } else if (!next.isInstantiated()) {
                throw new FutureInstantiationError(next);
            } else {
                throw new FutureTypeError(Interned.LIST_TYPE, origList);
            }
        }

        @Override
        public Term next() {
            if (!hasNext()) {
                throw new InternalError("Iterating at end of list");
            }
            CompoundTerm node = (CompoundTerm) next;
            Term term = node.get(0);
            next = node.get(1);
            return term;
        }
    }

    /**
     * Utility to extract list
     *
     * @param list Term to query
     * @return array of list elements
     */
    static List<Term> extractList(Term list) {
        WorkingTermList partial = compactList(list);
        if (partial.isConcrete()) {
            return partial.asList();
        } else {
            throw new FutureInstantiationError(list);
        }
    }

    /**
     * Utility to extract partial list (may have a tail that is unspecified)
     *
     * @param list Term to query
     * @return TermList filled as far as possible
     */
    static WorkingTermList compactList(Term list) {
        Term origList = list;
        ArrayList<Term> arr = new ArrayList<>();
        if (list instanceof WorkingTermList) {
            WorkingTermList self = (WorkingTermList)list;
            if (self.isConcrete()) {
                return self;
            }
        }
        while (list != PrologEmptyList.EMPTY_LIST) {
            if (!list.isInstantiated()) {
                // limit of what can be resolved
                if (arr.size() == 0) {
                    return new WorkingTermListTail(list);
                } else {
                    return new TermListImpl(arr, list);
                }
            }
            if (list instanceof TermList) {
                ((TermList) list).copyMembers(arr);
                list = ((TermList) list).lastTail();
            } else if (isList(list)) {
                arr.add(((CompoundTerm) list).get(0));
                list = ((CompoundTerm) list).get(1);
            } else {
                throw new FutureTypeError(Interned.LIST_TYPE, origList);
            }
        }
        if (arr.size() == 0) {
            return PrologEmptyList.EMPTY_LIST;
        } else {
            return new TermListImpl(arr, PrologEmptyList.EMPTY_LIST);
        }
    }

    /**
     * Convert Java list to a Prolog list
     *
     * @param list Java list
     * @return Prolog list
     */
    static WorkingTermList from(List<? extends Term> list) {
        if (list.isEmpty()) {
            return PrologEmptyList.EMPTY_LIST;
        } else {
            return new TermListImpl(list, PrologEmptyList.EMPTY_LIST);
        }
    }

    /**
     * Convenient way of describing an empty list
     *
     * @return Empty list
     */
    static Term empty() {
        return PrologEmptyList.EMPTY_LIST;
    }

}
