// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.bootstrap.Interned;
import prolog.constants.Atomic;
import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.LocalContext;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.predicates.ClauseEntry;
import prolog.predicates.ClauseSearchPredicate;
import prolog.predicates.PredicateDefinition;
import prolog.predicates.Predication;
import prolog.unification.Unifier;
import prolog.unification.UnifyBuilder;

/**
 * Find and retract matching clauses in a predicate.
 */
public class ExecRetractClause implements Instruction {
    private final Term clause;

    /**
     * Construct instruction with parameter to retract
     *
     * @param clause Parameter to retract
     */
    public ExecRetractClause(Term clause) {
        this.clause = clause;
    }

    /**
     * Iterate first clause in list of user-defined clauses
     *
     * @param environment Execution environment
     */
    @Override
    public void invoke(Environment environment) {
        LocalContext context = environment.getLocalContext();
        Term bound = clause.resolve(context);
        Term head;
        Term body;
        if (CompoundTerm.termIsA(bound, Interned.CLAUSE_FUNCTOR, 2)) {
            CompoundTerm compound = (CompoundTerm) bound;
            head = compound.get(0);
            body = compound.get(1);
        } else {
            head = bound;
            body = null; // indicate is was not specified
        }
        CompoundTerm matcher;
        //
        // Head must be sufficiently instantiated to build a matcher
        //
        if (head instanceof Atomic) {
            // Normalize matcher to a compound of arity 0
            matcher = CompoundTerm.from((Atomic) head);
        } else if (head instanceof CompoundTerm) {
            matcher = (CompoundTerm) head;
        } else {
            environment.backtrack();
            return;
        }
        Predication predication = new Predication(matcher.functor(), matcher.arity());
        PredicateDefinition defn = environment.lookupPredicate(predication);
        if (!(defn instanceof ClauseSearchPredicate)) {
            // Builtin or undefined
            environment.backtrack();
            return;
        }
        ClauseIterator iter =
                new ClauseIterator(environment, ((ClauseSearchPredicate) defn).getClauses(), matcher, body);
        iter.next();
    }

    /**
     * Main clause iterator with state. This is kept on the backtracking stack.
     */
    protected static class ClauseIterator extends DecisionPoint {

        final ClauseEntry[] clauses;
        final Term matcher;
        final Term body;
        final Unifier bodyUnifier;
        int index = 0;

        private ClauseIterator(Environment environment, ClauseEntry[] clauses, Term matcher, Term body) {
            super(environment);
            this.clauses = clauses;
            this.matcher = matcher;
            this.body = body;
            this.bodyUnifier = body == null ? null : UnifyBuilder.from(body); // compile this as the reference
        }

        /**
         * Iterate through clauses
         */
        @Override
        protected void next() {
            if (index == clauses.length) {
                // final fail
                environment.backtrack();
                return;
            }
            // Next clause
            ClauseEntry entry = clauses[index++];
            if (index != clauses.length) {
                // Backtracking will try another clause
                decisionContext.pushDecision(this);
            }
            LocalContext context = environment.newLocalContext();
            Term boundBody = bodyUnifier == null ? null : entry.getBody().resolve(context);

            // Attempt to unify
            Unifier unifier = entry.getUnifier();
            if (unifier.unify(context, matcher) &&
                    (bodyUnifier == null || bodyUnifier.unify(context, boundBody))) {
                // Once unified, remove!
                entry.getNode().remove();
                environment.forward();
            } else {
                // failed to unify, keep backtracking (will re-enter this function)
                environment.backtrack();
            }
        }
    }
}
