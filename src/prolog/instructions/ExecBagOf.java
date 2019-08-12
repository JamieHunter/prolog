// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.bootstrap.Interned;
import prolog.constants.PrologEmptyList;
import prolog.execution.EnumTermStrategy;
import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.expressions.CompoundTerm;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;
import prolog.expressions.TermList;
import prolog.expressions.TermListImpl;
import prolog.unification.Unifier;
import prolog.variables.Variable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

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

        //
        // decompose callable to identify the free variables
        // build a new template that collects the free variables
        //
        FreeVariableCollector fvc = new FreeVariableCollector(environment);
        template.enumTerm(fvc); // identify existential variables in the template
        callable = collectFreeVariables(fvc, callable); // any additional existential and free variables
        CompoundTerm freeVariables = new CompoundTermImpl(Interned.DOT, fvc.getFreeVariables());
        CompoundTerm combined = new CompoundTermImpl(Interned.CAROT_FUNCTOR, template, freeVariables);

        BagOfCollector iter = new BagOfCollector(environment, combined, callable, listUnifier);
        iter.next();
    }

    /**
     * Helper method to determine existential vs free variables of callable, supporting the Term^Goal syntax.
     * @param context Collector class
     * @param callable Originally specified Goal
     * @return Actual goal
     */
    protected Term collectFreeVariables(FreeVariableCollector context, Term callable) {
        while (CompoundTerm.termIsA(callable, Interned.CAROT_FUNCTOR, 2)) {
            CompoundTerm compound = (CompoundTerm) callable;
            compound.get(0).enumTerm(context); // copy & throw away, side-effects only
            callable = compound.get(1); // keep processing the right
        }
        context.beginFreeVariables();
        callable = callable.enumTerm(context); // right of X^Y^xxx construct
        return callable;
    }

    /**
     * Override to perform collation (setof). BagOf version does nothing.
     *
     * @param source List of terms
     * @return original list, or collated list if overridden.
     */
    protected List<Term> collate(List<Term> source) {
        return source;
    }

    // Variation of FindAllCollector with a different terminal operation
    private class BagOfCollector extends FindAllCollector {
        final Term realTemplate;
        final CompoundTerm freeVariables;
        public BagOfCollector(Environment environment, CompoundTerm combinedTemplate, Term callable,
                              Unifier listUnifier) {
            super(environment, combinedTemplate, callable, listUnifier);
            realTemplate = combinedTemplate.get(0);
            freeVariables = (CompoundTerm)combinedTemplate.get(1);
        }

        @Override
        protected void onDone(List<Term> solutions) {
            // chain to a new decision point iterator to produce collated solutions
            ProduceIterator iter = new ProduceIterator(environment, realTemplate, freeVariables, solutions, listUnifier);
            iter.next();
        }
    }

    /**
     * This decision-point iterator produces a new solution with a new set of free-variables on each iteration.
     */
    private class ProduceIterator extends DecisionPoint {

        final Term template;
        final CompoundTerm freeVariables;
        final Unifier listUnifier;
        final TreeMap<CompoundTerm, List<Term>> collated = new TreeMap<>();
        final Iterator<Map.Entry<CompoundTerm, List<Term>>> iter;

        protected ProduceIterator(Environment environment, Term template, CompoundTerm freeVariables,
                                  List<Term> solutions, Unifier listUnifier) {
            super(environment);
            this.template = template;
            this.freeVariables = freeVariables;
            this.listUnifier = listUnifier;

            // Collate solutions
            for(Term s : solutions) {
                CompoundTerm ct = (CompoundTerm)s;
                Term v = ct.get(0); // matches with template
                CompoundTerm k = (CompoundTerm)ct.get(1); // matches with free variables
                List<Term> list = collated.computeIfAbsent(k, kk -> new ArrayList<>());
                list.add(v);
            }
            iter = collated.entrySet().iterator();
        }

        @Override
        protected void next() {
            if (!iter.hasNext()) {
                // no more values
                environment.backtrack();
                return;
            }
            environment.forward();

            Map.Entry<CompoundTerm, List<Term>> entry = iter.next();
            CompoundTerm freeVarVals = entry.getKey();
            // entry will always contain at least one value, no need to handle an empty list.
            TermList values = new TermListImpl(collate(entry.getValue()), PrologEmptyList.EMPTY_LIST);
            environment.pushDecisionPoint(this);

            // Unify the list first
            if(!listUnifier.unify(environment.getLocalContext(), values)) {
                environment.backtrack();
                return;
            }

            // Unify succeeded, now bind all the free variables
            for (int i = 0; i < freeVariables.arity(); i++) {
                Term fv = freeVariables.get(i);
                Term fvv = freeVarVals.get(i);
                if (!fv.instantiate(fvv)) {
                    throw new InternalError("Unexpected");
                }
            }
        }
    }

    /**
     * used with Term enumeration capability to identify all free variables.
     */
    protected class FreeVariableCollector extends EnumTermStrategy {

        // TODO: Refactoring of EnumTermStrategy
        protected boolean beginFreeVariables = false;
        protected final ArrayList<Term> freeVariables = new ArrayList<>();

        public FreeVariableCollector(Environment environment) {
            super(environment);
        }

        @Override
        public Term visit(Term src, Function<? super Term, ? extends Term> mappingFunction) {
            return src;
        }

        @Override
        public CompoundTerm visit(CompoundTerm compound) {
            return compound.enumCompoundTerm(this);
        }

        public void beginFreeVariables() {
            beginFreeVariables = true;
        }

        public List<Term> getFreeVariables() {
            return freeVariables;
        }

        @Override
        public Variable visitVariable(Variable source) {
            boolean seen = hasVariable(source);
            Variable bound = bindVariable(source);
            if (beginFreeVariables && !seen) {
                freeVariables.add(bound);
            }
            return bound;
        }
    }
}
