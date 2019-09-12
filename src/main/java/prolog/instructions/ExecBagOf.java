// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.bootstrap.Interned;
import prolog.constants.PrologEmptyList;
import prolog.enumerators.VariableCollector;
import prolog.enumerators.CopyTerm;
import prolog.execution.DecisionPointImpl;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.expressions.CompoundTerm;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;
import prolog.expressions.TermList;
import prolog.expressions.TermListImpl;
import prolog.generators.DoRedo;
import prolog.unification.Unifier;
import prolog.unification.UnifyBuilder;
import prolog.variables.UnboundVariable;
import prolog.variables.Variable;

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
        private final Unifier varUnifier;
        private final List<Term> solutions;
        private int iter = 0;

        protected ProduceIterator(Environment environment, CompoundTerm freeVariables,
                                  List<Term> solutions, Unifier listUnifier) {
            super(environment);
            this.varUnifier = UnifyBuilder.from(freeVariables);
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

            // Collect possible solutions - solutions may be merged together including unification.
            // original logic just used sort, but that is not sufficient
            CompoundTerm entry = (CompoundTerm) solutions.get(iter++).resolve(context);
            CompoundTerm freeVarVals = (CompoundTerm) entry.get(1);

            // bind key with free variables
            if (!varUnifier.unify(context, freeVarVals)) {
                throw new InternalError("Unexpected unify failure");
            }

            // dedup/collate all other viable entries that can be considered to have the same set of values
            ArrayList<Term> values = new ArrayList<>();
            values.add(entry.get(0));
            for (int i = iter; i < solutions.size(); i++) {
                Term next = solutions.get(i);
                if (next == PrologEmptyList.EMPTY_LIST) {
                    continue;
                }
                int depth = environment.getBacktrackDepth();
                CompoundTerm compNext = (CompoundTerm) next.resolve(context);
                if (varUnifier.unify(context, compNext.get(1))) {
                    values.add(compNext.get(0));
                    solutions.set(i, PrologEmptyList.EMPTY_LIST); // don't include again
                } else {
                    // localized rollback
                    environment.trimBacktrackStackToDepth(depth);
                }
            }
            // entry will always contain at least one value, no need to handle an empty list.
            TermList outTerm = new TermListImpl(collate(values), PrologEmptyList.EMPTY_LIST);
            // Unify
            if (!listUnifier.unify(context, outTerm)) {
                environment.backtrack();
                return;
            }
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
        protected Variable renameVariable(Variable source) {
            return varMap.computeIfAbsent(source.id(), id -> {
                if (freeVariableIds.contains(id)) {
                    return source;
                } else {
                    return new UnboundVariable(source.name(), environment().nextVariableId());
                }
            });
        }

    }
}
