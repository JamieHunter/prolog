// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.bootstrap.Interned;
import org.jprolog.constants.PrologEmptyList;
import org.jprolog.enumerators.CopyTerm;
import org.jprolog.enumerators.VariableCollector;
import org.jprolog.execution.DecisionPointImpl;
import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.CompoundTermImpl;
import org.jprolog.expressions.Term;
import org.jprolog.expressions.TermList;
import org.jprolog.generators.DoRedo;
import org.jprolog.unification.Unifier;
import org.jprolog.unification.UnifyBuilder;
import org.jprolog.variables.ActiveVariable;
import org.jprolog.variables.Variable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extended/modified behavior from FindAll. This version understands free variables vs existential variables,
 * supporting a Term^Goal syntax to introduce additional existential variables (not needed in findall). Once
 * all solutions have been found (findall portion), they are collated into unique sets of free variables to
 * produce one or more solutions grouped by the free variables.
 */
public class ExecBagOf extends ExecFindAll {

    public ExecBagOf(Term template, Term callable, Term list) {
        super(template, callable, list);
    }

    /**
     * Perform findall then collate
     *
     * @param environment Execution environment
     */
    @Override
    protected void invoke2(Environment environment, Term template, Term callable) {
        // This will collect all solutions
        ArrayList<Term> builder = new ArrayList<>();

        //
        // decompose callable to identify the free variables
        // build a new template that collects the free variables
        //
        VariableCollector fvc = new VariableCollector(environment, VariableCollector.Mode.IGNORE);
        template.enumTerm(fvc); // identify existential variables in the template
        Term modifiedCallable = collectFreeVariables(fvc, callable); // any additional existential and free variables
        CompoundTerm freeVariables = new CompoundTermImpl(Interned.DOT, fvc.getVariables());
        CompoundTerm combinedTemplate = new CompoundTermImpl(Interned.CAROT_FUNCTOR, template, freeVariables);
        final Set<Long> freeVariableIds = new HashSet<Long>();
        for (int i = 0; i < freeVariables.arity(); i++) {
            freeVariableIds.add(((Variable) freeVariables.get(i)).id());
        }

        DoRedo.invoke(environment,
                // Perform the findAll phase and obtain all solutions. Unlike findall, this also considers
                // free variables.
                () -> getSourceSolutions(environment, modifiedCallable, () -> {
                    builder.add(combinedTemplate.enumTerm(new BagOfCopyTerm(environment, freeVariableIds)));
                }),
                // Once the find-all portion completes, move to the production phase, which will yield multiple
                // solutions
                () -> {
                    new ProduceIterator(environment, freeVariables, builder, listUnifier).redo();
                });
    }

    /**
     * Helper method to determine existential vs free variables of callable, supporting the Term^Goal syntax.
     *
     * @param context  Collector class
     * @param callable Originally specified Goal
     * @return Actual goal
     */
    protected Term collectFreeVariables(VariableCollector context, Term callable) {
        while (CompoundTerm.termIsA(callable, Interned.CAROT_FUNCTOR, 2)) {
            CompoundTerm compound = (CompoundTerm) callable;
            compound.get(0).enumTerm(context); // copy & throw away, side-effects only
            callable = compound.get(1); // keep processing the right
        }
        context.setMode(VariableCollector.Mode.COLLECT);
        callable = callable.enumTerm(context); // right of X^Y^xxx construct
        return callable;
    }

    /**
     * Override to perform collation (setof). BagOf version does nothing.
     *
     * @param source List of terms
     * @return original list, or collated list if overridden.
     */
    protected ArrayList<Term> collate(ArrayList<Term> source) {
        return source;
    }

    /**
     * This decision-point iterator produces a new solution with a new set of free-variables on each iteration.
     */
    private class ProduceIterator extends DecisionPointImpl {

        private final Unifier listUnifier;
        private final CompoundTerm freeVariables;
        private final List<Term> solutions;
        private int iter = 0;

        protected ProduceIterator(Environment environment, CompoundTerm freeVariables,
                                  List<Term> solutions, Unifier listUnifier) {
            super(environment);
            this.freeVariables = freeVariables;
            this.listUnifier = listUnifier;
            this.solutions = solutions;
        }

        @Override
        public void redo() {
            while (iter < solutions.size() && solutions.get(iter) == PrologEmptyList.EMPTY_LIST) {
                iter++;
            }
            if (iter == solutions.size()) {
                // no more values
                environment.backtrack();
                return;
            }
            environment.forward();
            environment.pushDecisionPoint(this);
            LocalContext context = environment.getLocalContext();
            HashSet<Long> bound = new HashSet<>();
            ArrayList<Term> values = new ArrayList<>();
            for (int i = iter; i < solutions.size(); i++) {
                Term next = solutions.get(i);
                if (next == PrologEmptyList.EMPTY_LIST) {
                    // skip anything previously picked
                    continue;
                }
                CompoundTerm compNext = (CompoundTerm) next.resolve(context);
                CompoundTerm solnValues = (CompoundTerm)compNext.get(1);
                if (tryMatch(solnValues,bound)) {
                    values.add(compNext.get(0));
                    solutions.set(i, PrologEmptyList.EMPTY_LIST); // don't include again
                    if (i == iter) {
                        iter++;
                    }
                }
            }
            if (values.size() == 0) {
                throw new InternalError("No values collected");
            }
            Term outTerm = TermList.from(collate(values)).toTerm();
            // Unify
            if (!listUnifier.unify(context, outTerm)) {
                environment.backtrack();
                return;
            }
        }

        /**
         * Succeeds if either free variables contain the same value as before, or if the free variables can be assigned
         * new values not previously seen before
         * @param solnValues Values in this solution
         * @param bound Set identifying what free variables have been unified
         * @return true if solution can be included
         */
        protected boolean tryMatch(CompoundTerm solnValues, Set<Long> bound) {
            List<Long> newBound = new ArrayList<>();
            int depth = environment.getBacktrackDepth();
            for(int j = 0; j < freeVariables.arity(); j++) {
                ActiveVariable freeVariable = (ActiveVariable)freeVariables.get(j);
                Term solnValue = solnValues.get(j);
                if (freeVariable.compareTo(solnValue) != 0) {
                    // pick? make sure free variable is not already picked and unify it
                    if (bound.contains(freeVariable.id()) || !freeVariable.instantiate(solnValue)) {
                        environment.trimBacktrackStackToDepth(depth);
                        return false;
                    } else {
                        newBound.add(freeVariable.id());
                    }
                }
            }
            bound.addAll(newBound);
            return true;
        }
    }

    /**
     * Like copyterm, but free variables remain bound to the original free variables
     */
    protected static class BagOfCopyTerm extends CopyTerm {

        protected final Set<Long> freeVariableIds;

        public BagOfCopyTerm(Environment environment, Set<Long> freeVariableIds) {
            super(environment);
            this.freeVariableIds = freeVariableIds;
        }


        /**
         * Rename only variables that are not in the set of free variables
         *
         * @param source Source variable
         * @return Renamed variable (deduped)
         */
        protected ActiveVariable renameVariable(ActiveVariable source) {
            if (freeVariableIds.contains(source.id())) {
                return source;
            } else {
                return super.renameVariable(source);
            }
        }

    }
}
