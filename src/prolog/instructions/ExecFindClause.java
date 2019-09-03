// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.bootstrap.Interned;
import prolog.constants.Atomic;
import prolog.exceptions.PrologInstantiationError;
import prolog.exceptions.PrologPermissionError;
import prolog.exceptions.PrologTypeError;
import prolog.execution.DecisionPointImpl;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.LocalContext;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.predicates.BuiltInPredicate;
import prolog.predicates.ClauseEntry;
import prolog.predicates.ClauseSearchPredicate;
import prolog.predicates.PredicateDefinition;
import prolog.predicates.Predication;
import prolog.unification.Unifier;
import prolog.unification.UnifyBuilder;

/**
 * Find and enumerate matching clauses in a predicate.
 */
public class ExecFindClause implements Instruction {
    private final Term head;
    private final Term body;

    public ExecFindClause(Term head, Term body) {
        this.head = head;
        this.body = body;
    }

    /**
     * Iterate first clause in list of user-defined clauses
     *
     * @param environment Execution environment
     */
    @Override
    public void invoke(Environment environment) {
        LocalContext context = environment.getLocalContext();
        Term boundHead = head.resolve(context);
        Term boundBody = body.resolve(context);
        CompoundTerm matcher;

        //
        // Head must be sufficiently instantiated to build a matcher
        //
        if (!boundHead.isInstantiated()) {
            throw PrologInstantiationError.error(environment, boundHead);
        }
        if (boundHead.isAtom()) {
            // Normalize matcher to a compound of arity 0
            matcher = CompoundTerm.from((Atomic) boundHead);
        } else if (boundHead instanceof CompoundTerm) {
            matcher = (CompoundTerm) boundHead;
        } else {
            throw PrologTypeError.callableExpected(environment, boundHead);
        }
        //
        // Body, if instantiated, must be something that can be called
        //
        if (boundBody.isInstantiated() && !(boundBody.isAtom() || boundBody instanceof CompoundTerm)) {
            throw PrologTypeError.callableExpected(environment, boundBody);
        }
        Predication key = new Predication(matcher.functor(), matcher.arity());
        PredicateDefinition defn = environment.lookupPredicate(key);
        if (defn instanceof BuiltInPredicate) {
            throw PrologPermissionError.error(environment,
                    Interned.ACCESS_ACTION, Interned.PRIVATE_PROCEDURE_TYPE, key.term(),
                    String.format("Cannot retrieve clause for internal procedure: %s", key.toString()));
        }
        if (!(defn instanceof ClauseSearchPredicate)) {
            environment.backtrack();
            return;
        }
        ClauseIterator iter =
                new ClauseIterator(environment, ((ClauseSearchPredicate) defn).getClauses(), matcher, boundBody);
        iter.redo();
    }

    /**
     * Clause iterator decision point.
     */
    private static class ClauseIterator extends DecisionPointImpl {

        final ClauseEntry[] clauses;
        final Term matcher;
        final Term body;
        final Unifier bodyUnifier;
        int index = 0;

        ClauseIterator(Environment environment, ClauseEntry[] clauses, Term matcher, Term body) {
            super(environment);
            this.clauses = clauses; // copy already made
            this.matcher = matcher;
            this.body = body;
            this.bodyUnifier = UnifyBuilder.from(body); // compile this as the reference
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void redo() {
            if (index == clauses.length) {
                // final fail
                environment.backtrack();
                return;
            }
            // Next clause
            ClauseEntry entry = clauses[index++];
            if (index != clauses.length) {
                // Backtracking will try another clause
                environment.pushDecisionPoint(this);
            }
            LocalContext context = environment.newLocalContext();
            Term boundBody = entry.getBody().resolve(context);

            // Attempt to unify head
            Unifier unifier = entry.getUnifier();
            if (unifier.unify(context, matcher) &&
                    bodyUnifier.unify(context, boundBody)) {
                // Unified, success
                environment.forward();
            } else {
                // failed to unify, keep backtracking (will re-enter this function)
                environment.backtrack();
            }
        }
    }
}
