// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.execution.CutPoint;
import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.InstructionPointer;
import prolog.execution.LocalContext;
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
    private static class ClauseIterator extends DecisionPoint implements CutPoint {

        final Term term;
        final Predication key;
        final ClauseEntry[] clauses;
        int index = 0;
        int cutDepth;

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
            environment.setLocalContext(decisionContext);
            environment.setCatchPoint(catchPoint);
            environment.setCutPoint(cutPoint);
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
            // Local context to use for execution of this clause
            LocalContext newContext = environment.newLocalContext(key); // chains cut-point from newContext to active
            environment.setLocalContext(newContext);
            if (index != clauses.length) {
                cutDepth = environment.getBacktrackDepth(); // might be reduced scope vs parent cut scope
                environment.pushDecisionPoint(this); // updates parent cut scope as needed
                environment.setCutPoint(this); // this object will handle cut for newContext
            } else {
                environment.setCutPoint(newContext); // tail-call style cut handling
            }
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
            if (unifier.unify(newContext, term)) {
                // Once unified, now execute, assume forward
                environment.forward();
                entry.getInstruction().invoke(environment); // this will push ClauseEnd onto stack
            } else {
                // failed to unify, keep backtracking (will re-enter this function)
                environment.backtrack();
            }
        }

        @Override
        public void cut() {
            LocalContext context = environment.getLocalContext();
            context.cut(); // cut local context first (marks this as deterministic)
            environment.cutBacktrackStack(cutDepth); // remove this decision point
            environment.setCutPoint(context); // future cut only needs to work on context
        }

        @Override
        public void markDecisionPoint(int depth) {
            // delegate to localContext
            environment.getLocalContext().markDecisionPoint(depth);
        }

        @Override
        public boolean isDeterministic() {
            // delegate to localContext
            return environment.getLocalContext().isDeterministic();
        }
    }
}
