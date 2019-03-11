// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.predicates;

import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.LocalContext;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;

/**
 * A predicate that is built in to Prolog that compiles a simple instruction with two terms
 */
public class BuiltinPredicateArity2 extends BuiltInPredicate {
    private final Lambda lambda;

    /**
     * Lambda of arity 2
     */
    @FunctionalInterface
    public interface Lambda {
        void call(Environment environment, Term a, Term b);
    }

    /**
     * Proxy around lambda to invoke lambda in response to instruction.
     */
    private static class Proxy implements Instruction {
        private final Lambda lambda;
        private final Term a;
        private final Term b;
        Proxy(Lambda lambda, Term a, Term b) {
            this.lambda = lambda;
            this.a = a;
            this.b = b;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void invoke(Environment environment) {
            LocalContext context = environment.getLocalContext();
            Term aBound = this.a.resolve(context);
            Term bBound = this.b.resolve(context);
            lambda.call(environment, aBound, bBound);
        }
    }

    /**
     * Create predicate for Lambda instruction
     *
     * @param lambda Lambda instruction
     */
    public BuiltinPredicateArity2(Lambda lambda) {
        this.lambda = lambda;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void compile(Predication predication, CompileContext compiling, CompoundTerm term) {
        Term a = term.get(0);
        Term b = term.get(1);
        compiling.add(new Proxy(lambda, a, b));
    }
}
