// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Predicate;
import org.jprolog.constants.Atomic;
import org.jprolog.constants.PrologInteger;
import org.jprolog.exceptions.PrologDomainError;
import org.jprolog.exceptions.PrologInstantiationError;
import org.jprolog.exceptions.PrologTypeError;
import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.CompoundTermImpl;
import org.jprolog.expressions.Term;
import org.jprolog.expressions.TermList;
import org.jprolog.expressions.WorkingTermList;
import org.jprolog.generators.YieldSolutions;
import org.jprolog.unification.Unifier;
import org.jprolog.unification.UnifyBuilder;
import org.jprolog.variables.LabeledVariable;

import java.util.ArrayList;
import java.util.Arrays;
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
     * @param structTerm  Structure (left)
     * @param listTerm    List (right)
     */
    @Predicate("=..")
    public static void univ(Environment environment, Term structTerm, Term listTerm) {
        WorkingTermList structArr = null;
        WorkingTermList listArr = null;
        Term structFromList = null;

        if (structTerm.isInstantiated()) {
            // flatten structure into a list
            structArr = extractStructureToList(structTerm);
        }
        if (listTerm.isInstantiated()) {
            // validate list
            listArr = TermList.compactList(listTerm);
            if (structArr == null && !listArr.isConcrete()) {
                // Cannot handle a trailing variable if no structure was provided
                throw PrologInstantiationError.error(environment, listArr.getFinalTailList().toTerm());
            }
            if (listArr.isEmptyList()) {
                throw PrologDomainError.nonEmptyList(environment, listTerm);
            }
            Term head = listArr.getHead();
            WorkingTermList tail = listArr.getTailList();
            if (head.isInstantiated()) {
                // extra validations
                if (tail.isConcrete() && tail.concreteSize() == 0) {
                    if (!head.isAtomic()) {
                        throw PrologTypeError.atomicExpected(environment, head);
                    }
                    structFromList = head;
                } else {
                    if (!head.isAtom()) {
                        throw PrologTypeError.atomExpected(environment, head);
                    }
                    if (structArr == null) {
                        // only create if needed
                        structFromList = new CompoundTermImpl((Atomic) head, tail.asList());
                    }
                }
            }
        }
        if (structArr != null && listArr != null) {
            // In this case, both the structure and the list were extracted and can be unified element by element
            Unifier.unifyLists(environment, structArr, listArr);
        } else if (structFromList != null) {
            // list is used to construct a structure
            Unifier.unifyTerm(environment, structTerm, structFromList);
        } else if (structArr != null) {
            // build list
            Unifier.unifyList(environment, listTerm, structArr);
        } else {
            // unsolvable
            throw PrologInstantiationError.error(environment, structTerm);
        }
    }

    /**
     * Compute length from list, or, compute list from length
     *
     * @param environment Execution environment
     * @param listTerm    Array list (weak)
     * @param lengthTerm  Length of list
     */
    @Predicate("length")
    public static void length(Environment environment, Term listTerm, Term lengthTerm) {
        if (lengthTerm.isInstantiated() && !listTerm.isInstantiated()) {
            // case 1, generate list from length
            int specifiedLength = PrologInteger.from(lengthTerm).notLessThanZero().toInteger();
            // compute anonymous list
            Term[] members = new Term[specifiedLength];
            LocalContext context = environment.getLocalContext();
            for (int i = 0; i < members.length; i++) {
                members[i] = new LabeledVariable("_", environment.nextVariableId()).
                        resolve(context);
            }
            Unifier.unifyTerm(environment, listTerm, TermList.from(Arrays.asList(members)).toTerm());
        } else if (listTerm.isInstantiated()) {
            // case 2, unify lengths if list is specified
            Unifier.unifyInteger(environment, lengthTerm, length(listTerm));
        } else {
            // case 3, unsolvable
            throw PrologInstantiationError.error(environment, listTerm);
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
        Unifier memberUnifier = UnifyBuilder.from(element); // worth compiling
        YieldSolutions.forAll(environment, listElements.stream(), thisElement ->
                thisElement.instantiate(element) || memberUnifier.unify(context, thisElement));
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
        return TermList.extractList(list).size();
    }

    /**
     * Utility to extract members of a structure
     *
     * @param struct Term assumed to be a structure, can be Atomic
     * @return array of structure elements
     */
    public static WorkingTermList extractStructureToList(Term struct) {
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
        return TermList.from(arr);
    }
}
