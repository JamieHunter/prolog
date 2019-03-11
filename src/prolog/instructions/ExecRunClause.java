// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.InstructionPointer;
import prolog.execution.RestoresLocalContext;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.predicates.ClauseEntry;
import prolog.predicates.ClauseSearchPredicate;
import prolog.predicates.Predication;
import prolog.unification.Unifier;

/**
 * Executes a predicate that consists of (or may consist of) a list of clauses.
 */
public class ExecRunClause implements Instruction {
    private final Predication predication;
    private final ClauseSearchPredicate predicate;
    private final CompoundTerm term;

    /**
     * Construct instruction to execute a user-defined predicate.
     *
     * @param predication Predication to put into LocalContext for case of error.
     * @param predicate   Matched predicate with clause definitions.
     * @param term        Term with parameters to be matched.
     */
    public ExecRunClause(Predication predication, ClauseSearchPredicate predicate, CompoundTerm term) {
        this.predication = predication;
        this.predicate = predicate;
        this.term = term;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invoke(Environment environment) {
        // Clauses are snapshot at time of call.
        ClauseIterator iter =
                new ClauseIterator(environment, predication, predicate.getClauses(), term.resolve(environment.getLocalContext()));
        iter.next();
    }

    /**
     * An instruction executed at end of a successful clause to restore stacks. This is deferred by pushing it onto
     * the IP stack. This is optimized for tail-call elimination.
     */
    private static class ClauseEnd implements RestoresLocalContext {

        final ClauseIterator iter;

        private ClauseEnd(ClauseIterator iter) {
            this.iter = iter;
        }

        @Override
        public void next() {
            // set up for resuming clause
            iter.handleEndOfClause();
        }

        @Override
        public InstructionPointer copy() {
            return this;
        }
    }

    /**
     * Main clause iterator with state. This is kept on the backtracking stack.
     */
    private static class ClauseIterator extends DecisionPoint {

        final Term term;
        final Predication key;
        final ClauseEntry[] clauses;
        int index = 0;

        private ClauseIterator(Environment environment, Predication key, ClauseEntry[] clauses, Term boundTerm) {
            super(environment);
            this.key = key;
            this.term = boundTerm; // save the bound version of this structure
            this.clauses = clauses; // already a copy, no need to clone
        }

        /**
         * Executed when clause has partially completed
         */
        void handleEndOfClause() {
            // Make sure to transition deterministic to not-deterministic as needed
            decisionContext.restoreCutDepth(environment.getLocalContext().getCutDepth());
            environment.setLocalContext(decisionContext);
            environment.setCatchPoint(catchPoint);
            environment.restoreIP();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void next() {
            if (index == clauses.length) {
                environment.backtrack();
                return;
            }
            // Next clause
            ClauseEntry entry = clauses[index++];
            if (index != clauses.length) {
                decisionContext.pushDecision(this);
            }
            // Local context to use for execution of this clause
            environment.setLocalContext(
                    environment.newLocalContext(key));
            // Tail-call detection
            if (environment.getIP() instanceof RestoresLocalContext) {
                // Eliminate below call. We know context and scope will be restored
                // in an outer context, so no need to restore it here
            } else {
                // on return, restore the context (can be eliminated)
                environment.callIP(new ClauseEnd(this));
            }

            // First attempt to unify
            Unifier unifier = entry.getUnifier();
            if (unifier.unify(environment.getLocalContext(), term)) {
                // Once unified, now execute, assume forward
                environment.forward();
                entry.getInstruction().invoke(environment); // this will push ClauseEnd onto stack
            } else {
                // failed to unify, keep backtracking (will re-enter this function)
                environment.backtrack();
            }
        }
    }
}
