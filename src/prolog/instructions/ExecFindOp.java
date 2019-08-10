// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.constants.Atomic;
import prolog.constants.PrologAtom;
import prolog.constants.PrologInteger;
import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.LocalContext;
import prolog.execution.OperatorEntry;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.predicates.ClauseEntry;
import prolog.predicates.ClauseSearchPredicate;
import prolog.predicates.PredicateDefinition;
import prolog.predicates.Predication;
import prolog.unification.Unifier;
import prolog.unification.UnifyBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Find and enumerate matching operators.
 */
public class ExecFindOp implements Instruction {
    private final Term nameTerm;
    private final Term precedenceTerm;
    private final Term typeTerm;

    public ExecFindOp(Term precedenceTerm, Term typeTerm, Term nameTerm) {
        this.precedenceTerm = precedenceTerm;
        this.typeTerm = typeTerm;
        this.nameTerm = nameTerm;
    }

    /**
     * Iterate first op in list of ops
     *
     * @param environment Execution environment
     */
    @Override
    public void invoke(Environment environment) {
        LocalContext context = environment.getLocalContext();
        Term boundName = nameTerm.resolve(context);
        Term boundPrecedence = precedenceTerm.resolve(context);
        Term boundType = typeTerm.resolve(context);

        PrologAtom constrainedName = null;
        OperatorEntry.Code constrainedType = null;
        if (boundName.isInstantiated()) {
            // most significant constraint
            constrainedName = PrologAtom.from(boundName);
        }
        if (boundType.isInstantiated()) {
            // at best, constrains between two search lists
            constrainedType = OperatorEntry.parseCode(PrologAtom.from(boundType));
        }
        if (boundPrecedence.isInstantiated()) {
            // type check, but not processed until iteration
            PrologInteger.from(boundPrecedence).get().intValue();
        }
        ArrayList<OperatorEntry> searchList = new ArrayList<>();
        // Apply constraints to search list. Ideally this will be one entry
        if (constrainedType != null) {
            if (constrainedType.isPrefix()) {
                addToList(searchList, constrainedName, environment.getPrefixOperators());
            } else {
                addToList(searchList, constrainedName, environment.getInfixPostfixOperators());
            }
        } else {
            addToList(searchList, constrainedName, environment.getPrefixOperators());
            addToList(searchList, constrainedName, environment.getInfixPostfixOperators());
        }
        OpIterator iter =
                new OpIterator(environment, searchList, boundPrecedence, boundType, boundName);
        iter.next();
    }

    /**
     * Builds up a list ready for iteration.
     * @param operators Target iteration list
     * @param name Name of operator (filter)
     * @param source Source map of operators
     */
    private static void addToList(List<OperatorEntry> operators, PrologAtom name, Map<Atomic,OperatorEntry> source) {
        if (name != null) {
            OperatorEntry select = source.get(name);
            if (select != null) {
                operators.add(select);
            }
        } else {
            operators.addAll(source.values());
        }
    }

    /**
     * Operator iterator decision point.
     */
    private static class OpIterator extends DecisionPoint {

        final ArrayList<OperatorEntry> operators;
        final Term precedenceTerm;
        final Term typeTerm;
        final Term nameTerm;
        final Unifier precedenceUnifier;
        final Unifier typeUnifier;
        final Unifier nameUnifier;
        int index = 0;

        OpIterator(Environment environment, ArrayList<OperatorEntry> operators,
                   Term precedenceTerm,
                   Term typeTerm,
                   Term nameTerm) {
            super(environment);
            this.operators = operators;
            this.precedenceTerm = precedenceTerm;
            this.typeTerm = typeTerm;
            this.nameTerm = nameTerm;
            this.precedenceUnifier = UnifyBuilder.from(precedenceTerm);
            this.typeUnifier = UnifyBuilder.from(typeTerm);
            this.nameUnifier = UnifyBuilder.from(nameTerm);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void next() {
            if (index == operators.size()) {
                // final fail
                environment.backtrack();
                return;
            }
            // Next operator
            OperatorEntry entry = operators.get(index++);
            if (index != operators.size()) {
                // Backtracking will try another operator
                // Must be added before unification below
                environment.pushDecisionPoint(this);
            }

            PrologInteger precedence = new PrologInteger(entry.getPrecedence());
            PrologAtom type = entry.getCode().atom();
            Atomic name = entry.getFunctor();
            LocalContext context = environment.getLocalContext();

            if (nameUnifier.unify(context, name) &&
                    typeUnifier.unify(context, type) &&
                    precedenceUnifier.unify(context, precedence)) {
                environment.forward();
            } else {
                environment.backtrack();
            }
        }
    }
}
