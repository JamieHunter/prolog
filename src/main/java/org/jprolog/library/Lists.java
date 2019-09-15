// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Interned;
import org.jprolog.bootstrap.Predicate;
import org.jprolog.constants.PrologEmptyList;
import org.jprolog.constants.PrologInteger;
import org.jprolog.exceptions.PrologDomainError;
import org.jprolog.exceptions.PrologInstantiationError;
import org.jprolog.exceptions.PrologRepresentationError;
import org.jprolog.exceptions.PrologTypeError;
import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.CompoundTermImpl;
import org.jprolog.expressions.Term;
import org.jprolog.expressions.TermList;
import org.jprolog.expressions.TermListImpl;
import org.jprolog.generators.YieldSolutions;
import org.jprolog.unification.Unifier;
import org.jprolog.unification.UnifyBuilder;
import org.jprolog.variables.LabeledVariable;

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
     * instantiated, the list being instantiated, or both updating each other.
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

        if (!struct.isInstantiated()) {
            if (!list.isInstantiated()) {
                throw PrologInstantiationError.error(environment, list);
            }
            if (!TermList.isList(list)) {
                throw PrologTypeError.listExpected(environment, list);
            }
        }
        if (struct.isInstantiated()) {
            structArr = extractStructure(struct);
            listFromStruct = new TermListImpl(
                    structArr.toArray(new Term[structArr.size()]),
                    PrologEmptyList.EMPTY_LIST);
        }
        if (list.isInstantiated()) {
            if (!struct.isInstantiated() && list == PrologEmptyList.EMPTY_LIST) {
                throw PrologDomainError.nonEmptyList(environment, list);
            }
            CompoundTerm listComp = (CompoundTerm) list;
            Term head = listComp.get(0);
            Term tail = listComp.get(1);
            if (!struct.isInstantiated() && !head.isInstantiated()) {
                throw PrologInstantiationError.error(environment, head);
            }
            if (head.isInstantiated() && !head.isAtomic() && tail == PrologEmptyList.EMPTY_LIST) {
                throw PrologTypeError.atomicExpected(environment, head);
            }
            if (head.isInstantiated() && !head.isAtom() && tail != PrologEmptyList.EMPTY_LIST) {
                throw PrologTypeError.atomExpected(environment, head);
            }
            if (!struct.isInstantiated()) {
                if (tail == PrologEmptyList.EMPTY_LIST) {
                    structFromList = head;
                } else {
                    listArr = TermList.extractList(list);
                    if ((listArr.size() - 1) > environment.getFlags().maxArity) {
                        throw PrologRepresentationError.error(environment, Interned.MAX_ARITY_REPRESENTATION);
                    }
                    structFromList = new CompoundTermImpl(
                            listArr.toArray(new Term[listArr.size()]));
                }
            }
        }
        if ((structFromList != null && !Unifier.unify(environment.getLocalContext(), struct, structFromList)) ||
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
            specifiedLength = PrologInteger.from(length).notLessThanZero().toInteger();
            // compute anonymous list
            Term[] members = new Term[specifiedLength];
            LocalContext context = environment.getLocalContext();
            for (int i = 0; i < members.length; i++) {
                members[i] = new LabeledVariable("_", environment.nextVariableId()).
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
            Term genLen = PrologInteger.from(calculatedLength);
            if (!Unifier.unify(environment.getLocalContext(), length, genLen)) {
                environment.backtrack();
            }
        } else if (!length.isInstantiated()) {
            environment.backtrack();
        }
    }

    /**
     * Determine if a term is a member of a list
     *
     * @param environment Execution environment
     * @param element     Term to test for
     * @param list        List to extract members of
     */
    @Predicate("member")
    public static void member(Environment environment, Term element, Term list) {
        LocalContext context = environment.getLocalContext();
        List<Term> listElements = TermList.extractList(list);
        Unifier memberUnifier = UnifyBuilder.from(element);
        YieldSolutions.forAll(environment, listElements.stream(), thisElement -> {
            if (thisElement.instantiate(element)) {
                return true;
            }
            return memberUnifier.unify(context, thisElement);
        });
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
