// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.expressions;

import prolog.bootstrap.Interned;
import prolog.constants.PrologAtomLike;
import prolog.constants.PrologCharacter;
import prolog.constants.PrologEmptyList;
import prolog.constants.PrologInteger;
import prolog.constants.PrologString;
import prolog.constants.PrologStringAsList;
import prolog.exceptions.FutureInstantiationError;
import prolog.exceptions.FutureTypeError;
import prolog.exceptions.PrologInstantiationError;
import prolog.exceptions.PrologTypeError;
import prolog.enumerators.EnumTermStrategy;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.unification.HeadTailUnifyIterator;
import prolog.unification.UnifyIterator;

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
     * Resolve list
     *
     * @param context Context for variable bindings
     * @return new list
     */
    @Override
    TermList resolve(LocalContext context);

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
     * @return Length of list contained in TermList, ignoring tail component
     */
    int length();

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

    TermList enumTerm(EnumTermStrategy strategy);

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
        Term origList = list;
        ArrayList<Term> arr = new ArrayList<>();
        while (list != PrologEmptyList.EMPTY_LIST) {
            if (!list.isInstantiated()) {
                throw new FutureInstantiationError(list);
            }
            if (list instanceof TermList) {
                ((TermList) list).copyMembers(arr);
                list = ((TermList) list).lastTail();
            } else if (isList(list)) {
                arr.add(((CompoundTerm) list).get(0));
                list = ((CompoundTerm) list).get(1);
            } else if (!list.isInstantiated()) {
                throw new FutureInstantiationError(list);
            } else {
                throw new FutureTypeError(Interned.LIST_TYPE, origList);
            }
        }
        return arr;
    }

    /**
     * Utility to extract list of code-points/code-characters to a string
     *
     * @param list Term to query of length at least one
     * @return PrologCodePoints
     */
    static String extractString(Environment environment, Term list) {
        Term origList = list;
        if (list == PrologEmptyList.EMPTY_LIST) {
            return "";
        }
        if (list instanceof PrologStringAsList) {
            return ((PrologStringAsList) list).getStringValue();
        }
        if (list instanceof PrologString) {
            return ((PrologString) list).get();
        }
        StringBuilder builder = new StringBuilder();
        while (list != PrologEmptyList.EMPTY_LIST) {
            if (list instanceof PrologStringAsList) {
                builder.append(((PrologStringAsList) list).getStringValue());
                return builder.toString();
            }
            if (list instanceof PrologString) {
                builder.append(((PrologString) list).get());
                return builder.toString();
            }
            if (isList(list)) {
                CompoundTerm comp = (CompoundTerm) list;
                Term e = comp.get(0);
                list = comp.get(1);
                if (e instanceof PrologCharacter) {
                    builder.append(((PrologCharacter) e).get());
                } else if (e instanceof PrologAtomLike) {
                    builder.append(((PrologAtomLike) e).name().charAt(0));
                } else if (e instanceof PrologInteger) {
                    builder.append(((PrologInteger) e).toChar());
                } else if (!e.isInstantiated()) {
                    throw PrologInstantiationError.error(environment, list);
                } else {
                    throw PrologTypeError.characterExpected(environment, e);
                }
            } else if (!list.isInstantiated()) {
                throw new FutureInstantiationError(list);
            } else {
                throw PrologTypeError.listExpected(environment, origList);
            }
        }
        return builder.toString();
    }

    /**
     * Convert Java list to a Prolog list
     *
     * @param list Java list
     * @return Prolog list
     */
    static Term from(List<Term> list) {
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
