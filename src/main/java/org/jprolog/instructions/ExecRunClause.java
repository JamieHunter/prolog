// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.bootstrap.Interned;
import org.jprolog.callstack.ResumableExecutionPoint;
import org.jprolog.callstack.TransferHint;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.cuts.ClauseCutBarrier;
import org.jprolog.exceptions.PrologExistenceError;
import org.jprolog.execution.DecisionPointImpl;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.execution.LocalContext;
import org.jprolog.execution.RestoresLocalContext;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.Term;
import org.jprolog.predicates.ClauseEntry;
import org.jprolog.predicates.ClauseSearchPredicate;
import org.jprolog.predicates.Predication;
import org.jprolog.unification.Unifier;

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
    private CompoundTerm reflect() {
        return term;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invoke(Environment environment) {
        ClauseEntry[] clauses = predicate.getClauses();
        if (clauses.length == 0 && !predicate.isDynamic() &&
                !predicate.isMultifile() &&
                !predicate.isDiscontiguous()) {
            Predication pred = new Predication(PrologAtomLike.from(term.functor()), term.arity());
            switch (environment.getFlags().unknown) {
                case ATOM_fail:
                    environment.backtrack();
                    return;
                case ATOM_warning:
                    System.err.format("(Warning) Undefined predicate: %s\n", pred.toString());
                    environment.backtrack();
                    return;
                default:
                    throw PrologExistenceError.error(environment,
                            Interned.PROCEDURE, pred.term(), String.format("Predicate %s not defined", pred.toString()), null);
            }
        }
        // Clauses are snapshot at time of call.
        ClauseIterator iter =
                new ClauseIterator(environment, reflect(), predication, clauses, term.resolve(environment.getLocalContext()));
        iter.redo();
    }

    /**
     * An instruction executed at end of a successful clause to restore stacks. This is deferred by pushing it onto
     * the IP stack. This is optimized for tail-call elimination.
     */
    private static class ClauseEnd implements RestoresLocalContext {

        final Environment environment;
        final ResumableExecutionPoint previous;
        final ClauseIterator iter;

        private ClauseEnd(Environment environment, ClauseIterator iter) {
            this.environment = environment;
            this.previous = environment.getExecution().freeze();
            this.iter = iter;
        }

        @Override
        public void invokeNext() {
            // set up for resuming clause
            environment.setExecution(previous, TransferHint.LEAVE);
            iter.handleEndOfClause();
        }

        @Override
        public Object id() {
            return this;
        }

        @Override
        public ResumableExecutionPoint previousExecution() {
            return previous;
        }
    }

    /**
     * Main clause iterator with state. This is kept on the backtracking stack.
     */
    private static class ClauseIterator extends DecisionPointImpl {

        final Term term;
        final Predication key;
        final CompoundTerm source;
        final ClauseEntry[] clauses;
        final long variableWatermark;
        int index = 0;

        private ClauseIterator(Environment environment, CompoundTerm source, Predication key, ClauseEntry[] clauses, Term boundTerm) {
            super(environment);
            this.source = source;
            this.key = key;
            this.term = boundTerm; // save the bound version of this structure
            this.clauses = clauses; // already a copy, no need to clone
            this.variableWatermark = environment.variableWatermark();
        }

        /**
         * Executed when clause has partially completed
         */
        void handleEndOfClause() {
            environment.setLocalContext(decisionContext);
            environment.setCatchPoint(catchPoint);
            environment.setCutPoint(cutPoint);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void redo() {
            if (index == clauses.length) {
                environment.backtrack();
                return;
            }
            // Next clause
            ClauseEntry entry = clauses[index++];
            // Local context to use for execution of this clause
            LocalContext newContext = environment.newLocalContext(key); // chains cut-point from newContext to active
            environment.setLocalContext(newContext);
            // cut point must be before decision point and captures backtrack mark before the decision point
            // making everything afterwards non-deterministic
            environment.setCutPoint(new ClauseCutBarrier(environment, environment.getCutPoint(), variableWatermark));
            if (index != clauses.length) {
                // not deterministic (this will introduce a new CutPoint entry)
                environment.pushDecisionPoint(this); // updates parent cut scope as needed
            }
            // Tail-call detection
            if (!(environment.getExecution() instanceof RestoresLocalContext)) {
                // on return, restore the context (can be eliminated if previous entry is RestoresLocalContext)
                environment.setExecution(new ClauseEnd(environment, this), TransferHint.CONTROL);
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

    }
}
