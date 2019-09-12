// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.bootstrap.Interned;
import prolog.enumerators.CopyTerm;
import prolog.exceptions.PrologTypeError;
import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.LocalContext;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.expressions.TermList;
import prolog.generators.DoRedo;
import prolog.unification.Unifier;
import prolog.unification.UnifyBuilder;

import java.util.ArrayList;

/**
 * Find all possible solutions of a subgoal. This is overridden for bagof and setof implementations.
 */
public class ExecFindAll implements Instruction {
    private final Term template;
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
        // This will collect all solutions
        ArrayList<Term> builder = new ArrayList<>();

        DoRedo.invoke(environment,
                // First iteration, execute the 'recursive' instructions compiled above
                () -> getSourceSolutions(environment, callable, () -> {
                    builder.add(template.enumTerm(new CopyTerm(environment)));
                }),
                // When callable finally fails, we're complete (executed in future)
                () -> {
                    if (!listUnifier.unify(environment.getLocalContext(), TermList.from(builder))) {
                        environment.backtrack();
                    }
                });
    }

    void getSourceSolutions(Environment environment, Term callable, Runnable append) {
        // recursive instructions
        CompileContext compile = environment.newCompileContext();
        compile.addCall(callable, new ExecCall(ExecBlock.nested(compile, callable)));
        compile.add(null, e -> {
            // When callable succeeds, collect the solution and backtrack
            append.run();
            environment.backtrack();
        });
        compile.toInstruction().invoke(environment);
    }
}
