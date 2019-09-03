// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.bootstrap.Interned;
import prolog.constants.PrologEmptyList;
import prolog.exceptions.PrologTypeError;
import prolog.execution.CompileContext;
import prolog.execution.CopyTerm;
import prolog.execution.DecisionPointImpl;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.LocalContext;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.expressions.TermListImpl;
import prolog.library.Control;
import prolog.unification.Unifier;
import prolog.unification.UnifyBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Find all possible solutions of a subgoal. This is overridden for bagof and setof implementations.
 */
public class ExecFindAll implements Instruction {
    protected final Term template;
    private final Term callable;
    private final Term list;
    protected final Unifier listUnifier;

    public ExecFindAll(Term template, Term callable, Term list) {
        this.template = template;
        this.callable = callable;
        this.list = list;
        this.listUnifier = UnifyBuilder.from(list);
    }

    /**
     * Execute instruction
     *
     * @param environment Execution environment
     */
    @Override
    public void invoke(Environment environment) {
        LocalContext context = environment.getLocalContext();
        // ground as far as possible
        Term boundTemplate = template.resolve(context);
        Term boundCallable = callable.resolve(context);
        Term boundList = list.resolve(context);

        // Validation of the target list before we start
        // Callable will be validated when compiled
        if (boundList.isInstantiated() && !CompoundTerm.termIsA(boundList, Interned.LIST_FUNCTOR, 2)) {
            throw PrologTypeError.listExpected(environment, boundList);
        }

        // findall vs bagof implementation
        invoke2(environment, boundTemplate, boundCallable);
    }

    protected void invoke2(Environment environment, Term template, Term callable) {
        FindAllCollector iter =
                new FindAllCollector(environment, template, callable, listUnifier);
        iter.redo();
    }

    /**
     * Iterate each possible solution to produce a list
     */
    protected class FindAllCollector extends DecisionPointImpl {

        protected final Term template;
        protected final Unifier listUnifier;
        private ArrayList<Term> builder = new ArrayList<>();
        private final Instruction call;

        FindAllCollector(Environment environment,
                         Term template,
                         Term callable,
                         Unifier listUnifier) {
            super(environment);
            this.listUnifier = listUnifier;
            this.template = template;
            CompileContext compile = environment.newCompileContext();
            compile.addCall(callable, new ExecCall(ExecBlock.nested(compile, callable)));
            compile.add(null, e -> {
                // success
                builder.add(copyTemplate());
                environment.backtrack();
            });
            compile.add(null, Control.FALSE);
            call = compile.toInstruction();
        }

        protected Term copyTemplate() {
            return template.enumTerm(new CopyTerm(environment));
        }

        @Override
        public void redo() {
            //
            // Attempt next possible solution (this will only run once)
            //
            environment.pushDecisionPoint(this);
            call.invoke(environment);
        }

        @Override
        public void backtrack() {
            super.restore();
            environment.forward();
            onDone(builder);
        }

        /**
         * Called on completion of findall
         *
         * @param result array of Terms produced
         */
        protected void onDone(List<Term> result) {
            Term collected;
            if (builder.size() == 0) {
                collected = PrologEmptyList.EMPTY_LIST;
            } else {
                collected = new TermListImpl(builder, PrologEmptyList.EMPTY_LIST);
            }
            if (!listUnifier.unify(environment.getLocalContext(), collected)) {
                environment.backtrack();
            }
        }
    }
}
