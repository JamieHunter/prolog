// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.predicates;

import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.Term;
import org.jprolog.execution.CompileContext;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.execution.LocalContext;

/**
 * A predicate that is built in to Prolog that compiles a simple instruction with one term
 */
public class BuiltinPredicateArity1 extends BuiltInPredicate {
    private final Lambda lambda;

    /**
     * Lambda of arity 1
     */
    @FunctionalInterface
    public interface Lambda {
        void call(Environment environment, Term a);
    }

    /**
     * Proxy around lambda to invoke lambda in response to instruction.
     */
    private static class Proxy implements Instruction {
        private final Lambda lambda;
        private final Term a;

        Proxy(Lambda lambda, Term a) {
            this.lambda = lambda;
            this.a = a;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void invoke(Environment environment) {
            LocalContext context = environment.getLocalContext();
            Term aBound = this.a.resolve(context);
            lambda.call(environment, aBound);
        }
    }

    /**
     * Create predicate for Lambda instruction
     *
     * @param lambda Lambda instruction
     */
    public BuiltinPredicateArity1(Lambda lambda) {
        this.lambda = lambda;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void compile(Predication predication, CompileContext compiling, CompoundTerm term) {
        Term a = term.get(0);
        compiling.add(term, new Proxy(lambda, a));
    }
}
