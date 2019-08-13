// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Interned;
import prolog.bootstrap.Predicate;
import prolog.constants.PrologEmptyList;
import prolog.constants.PrologInteger;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.expressions.CompoundTerm;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;
import prolog.expressions.TermList;
import prolog.expressions.TermListImpl;
import prolog.unification.Unifier;
import prolog.variables.UnboundVariable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Bootstraps list manipulation predicates.
 */
public final class Lists {
    private Lists() {
        // Static methods/fields only
    }

    /**
     * Unifies between a compound term and a list. This may result in the compound term being
     * instantiated, the list being instiated, or both updating each other.
     *
     * @param environment Execution environment
     * @param struct      Structure (left)
     * @param list        List (right)
     */
    @Predicate("=..")
    public static void univ(Environment environment, Term struct, Term list) {
        List<Term> structArr;
        List<Term> listArr;
        TermList listFromStruct = null;
        Term structFromList = null;
        if (struct.isInstantiated()) {
            // Always this path even if list is instantiated.
            structArr = extractStructure(struct);
            listFromStruct = new TermListImpl(
                    structArr.toArray(new Term[structArr.size()]),
                    PrologEmptyList.EMPTY_LIST);
        }
        if (list.isInstantiated() && !struct.isInstantiated()) {
            // only need to go down this path is structure is not instantiated, otherwise
            // the single unification will work well enough
            listArr = TermList.extractList(list);
            if (listArr.size() == 0) {
                throw new UnsupportedOperationException("Functor was not specified");
            }
            if (!listArr.get(0).isAtom()) {
                throw new UnsupportedOperationException("Functor must be an atom");
            }
            if (listArr.size() == 1) {
                structFromList = listArr.get(0);
            } else {
                structFromList = new CompoundTermImpl(
                        listArr.toArray(new Term[listArr.size()]));
            }
        }
        if ((structFromList == null && listFromStruct == null) ||
                (structFromList != null && !Unifier.unify(environment.getLocalContext(), struct, structFromList)) ||
                (listFromStruct != null && !Unifier.unify(environment.getLocalContext(), list, listFromStruct))) {
            environment.backtrack();
        }
    }

    /**
     * Compute length from list, or, compute list from length
     *
     * @param environment Execution environment
     * @param list        Array list (weak)
     * @param length      Length of list
     */
    @Predicate("length")
    public static void length(Environment environment, Term list, Term length) {
        int calculatedLength;
        int specifiedLength;
        if (length.isInstantiated() && !list.isInstantiated()) {
            // case 1, generate list from length
            specifiedLength = PrologInteger.from(length).get().intValue();
            if (specifiedLength < 0) {
                throw new IndexOutOfBoundsException("Length of list < 0");
            }
            // compute anonymous list
            Term[] members = new Term[specifiedLength];
            LocalContext context = environment.getLocalContext();
            for (int i = 0; i < members.length; i++) {
                members[i] = new UnboundVariable("_", environment.nextVariableId()).
                        resolve(context);
            }
            TermList genList = new TermListImpl(members, PrologEmptyList.EMPTY_LIST);
            if (!Unifier.unify(context, list, genList)) {
                environment.backtrack();
            }
            return;
        }
        if (list.isInstantiated()) {
            // case 2, unify lengths if list is specified
            calculatedLength = length(list);
            Term genLen = new PrologInteger(BigInteger.valueOf(calculatedLength));
            if (!Unifier.unify(environment.getLocalContext(), length, genLen)) {
                environment.backtrack();
            }
        } else if (!length.isInstantiated()) {
            environment.backtrack();
        }
    }

    // ====================================================================
    // Helper methods
    // ====================================================================

    /**
     * Utility to calculate length of list
     *
     * @param list List to query
     * @return length of list
     */
    public static int length(Term list) {
        int len = 0;
        while (list != PrologEmptyList.EMPTY_LIST) {
            if (list instanceof TermList) {
                len += ((TermList) list).length();
                list = ((TermList) list).lastTail();
            } else if (CompoundTerm.termIsA(list, Interned.LIST_FUNCTOR, 2)) {
                len++;
                list = ((CompoundTerm) list).get(1);
            } else {
                len++;
                break;
            }
        }
        return len;
    }

    /**
     * Utility to extract members of a structure
     *
     * @param struct Term assumed to be a structure, can be Atomic
     * @return array of structure elements
     */
    public static List<Term> extractStructure(Term struct) {
        ArrayList<Term> arr = new ArrayList<>();
        if (struct instanceof CompoundTerm) {
            CompoundTerm compound = (CompoundTerm) struct;
            arr.add(compound.functor());
            for (int i = 0; i < compound.arity(); i++) {
                arr.add(compound.get(i));
            }
        } else {
            arr.add(struct);
        }
        return arr;
    }
}
