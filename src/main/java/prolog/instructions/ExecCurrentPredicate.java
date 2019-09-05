// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.bootstrap.Interned;
import prolog.constants.PrologAtomInterned;
import prolog.constants.PrologAtomLike;
import prolog.constants.PrologInteger;
import prolog.exceptions.PrologDomainError;
import prolog.exceptions.PrologTypeError;
import prolog.execution.DecisionPointImpl;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.LocalContext;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.predicates.PredicateDefinition;
import prolog.predicates.Predication;
import prolog.unification.Unifier;
import prolog.unification.UnifyBuilder;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test a predicate is user defined, or enumerate all user-defined predicates.
 */
public class ExecCurrentPredicate implements Instruction {
    private final Term indicator;

    public ExecCurrentPredicate(Term indicator) {
        this.indicator = indicator;
    }

    /**
     * Iterate first clause in list of user-defined clauses
     *
     * @param environment Execution environment
     */
    @Override
    public void invoke(Environment environment) {
        LocalContext context = environment.getLocalContext();
        Term bound = indicator.resolve(context);
        PrologAtomLike functorConstraint = null;
        Integer arityConstraint = null;
        if (bound.isInstantiated()) {
            if (!CompoundTerm.termIsA(bound, Interned.SLASH_ATOM, 2)) {
                throw PrologTypeError.predicateIndicatorExpected(environment, bound);
            }
            CompoundTerm compoundTerm = (CompoundTerm) bound;
            Term functor = compoundTerm.get(0);
            Term arity = compoundTerm.get(1);
            if (functor.isInstantiated()) {
                if (functor.isAtom()) {
                    functorConstraint = PrologAtomInterned.from(environment, functor);
                } else {
                    throw PrologTypeError.predicateIndicatorExpected(environment, bound);
                }
            }
            if (arity.isInstantiated()) {
                if (arity.isInteger()) {
                    arityConstraint = PrologInteger.from(arity).notLessThanZero().toInteger();
                } else {
                    throw PrologTypeError.predicateIndicatorExpected(environment, bound);
                }
            }
        }

        Set<Predication.Interned> predications = filter(environment, functorConstraint, arityConstraint)
                .collect(Collectors.toCollection(TreeSet::new));
        if (predications.isEmpty()) {
            environment.backtrack();
            return;
        }
        PredicateIterator iter =
                new PredicateIterator(environment, bound, predications.iterator());
        iter.redo();
    }

    /**
     * Return a filtered stream of user defined predicates
     *
     * @param environment Execution environment
     * @param functor     Functor to filter, else null.
     * @param arity       Arity to filter, else null
     * @return stream of matching predications
     */
    protected static Stream<Predication.Interned> filter(Environment environment, PrologAtomLike functor, Integer arity) {
        Stream<Map.Entry<Predication.Interned, PredicateDefinition>> allPredicates;
        if (functor != null && arity != null) {
            Predication.Interned key = new Predication(functor, arity).intern(environment);
            PredicateDefinition singleDefinition = environment.lookupPredicate(key);
            if (singleDefinition == null || !singleDefinition.isCurrentPredicate()) {
                return Stream.empty();
            }
            return Stream.of(key);
        } else {
            return environment.getShared().allPredicates().filter(e ->
                    (functor == null || functor == e.getKey().functor()) &&
                            (arity == null || arity == e.getKey().arity()) &&
                            e.getValue().isCurrentPredicate())
                    .map(Map.Entry::getKey);
        }
    }

    /**
     * Predicate iterator decision point.
     */
    private static class PredicateIterator extends DecisionPointImpl {

        final Term indicator;
        final Unifier indicatorUnifier;
        final Iterator<Predication.Interned> predications;

        PredicateIterator(Environment environment, Term indicator, Iterator<Predication.Interned> predications) {
            super(environment);
            this.indicator = indicator;
            this.predications = predications;
            this.indicatorUnifier = UnifyBuilder.from(indicator);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void redo() {
            if (!predications.hasNext()) {
                // final fail
                environment.backtrack();
                return;
            }
            environment.forward();
            // Next predication
            Term thisTerm = predications.next().term();
            if (predications.hasNext()) {
                // Backtracking will try another clause
                environment.pushDecisionPoint(this);
            }
            if (!indicatorUnifier.unify(environment.getLocalContext(), thisTerm)) {
                environment.backtrack(); // not expected
            }
        }
    }
}
