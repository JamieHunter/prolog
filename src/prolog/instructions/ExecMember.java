// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.LocalContext;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.expressions.TermList;
import prolog.unification.Unifier;
import prolog.unification.UnifyBuilder;

import java.util.List;

/**
 * Find/enumerate elements within list
 */
public class ExecMember extends Traceable {
    private final Term element;
    private final Term list;

    public ExecMember(CompoundTerm source, Term element, Term list) {
        super(source);
        this.element = element;
        this.list = list;
    }

    /**
     * Execute member iterator instruction
     *
     * @param environment Execution environment
     */
    @Override
    public void invoke(Environment environment) {
        LocalContext context = environment.getLocalContext();
        Term boundElement = element.resolve(context);
        Term boundList = list.resolve(context);
        List<Term> listElements = TermList.extractList(boundList);
        Unifier memberUnifier = UnifyBuilder.from(boundElement);
        MemberIterator iter =
                new MemberIterator(environment, boundElement, memberUnifier, listElements);
        iter.next();
    }

    /**
     * Iterate each possible member and unify with target element
     */
    protected class MemberIterator extends DecisionPoint {

        private final Term searchElement;
        private final Unifier memberUnifier;
        private final List<Term> listElements;
        private int index = 0;

        MemberIterator(Environment environment,
                       Term searchElement,
                       Unifier memberUnifier,
                       List<Term> listElements) {
            super(environment);
            this.searchElement = searchElement;
            this.memberUnifier = memberUnifier;
            this.listElements = listElements;
        }

        @Override
        protected void next() {
            if (index >= listElements.size()) {
                // end of list
                environment.backtrack();
                return;
            }
            environment.forward();
            Term thisElement = listElements.get(index++);
            if (index < listElements.size()) {
                environment.pushDecisionPoint(this); // note that last element will not have a decision point
            }
            if (thisElement.instantiate(searchElement)) {
                return;
            }
            if (!memberUnifier.unify(environment.getLocalContext(), thisElement)) {
                environment.backtrack();
            }
        }
    }
}
