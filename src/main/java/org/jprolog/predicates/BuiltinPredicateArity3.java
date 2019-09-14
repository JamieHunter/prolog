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
 * A predicate that is built in to Prolog that compiles a simple instruction with three terms
 */
public class BuiltinPredicateArity3 extends BuiltInPredicate {
    private final Lambda lambda;

    /**
     * Lambda of arity 3
     */
    @FunctionalInterface
    public interface Lambda {
        void call(Environment environment, Term a, Term b, Term c);
    }

    /**
     * Proxy around lambda to invoke lambda in response to instruction.
     */
    private static class Proxy implements Instruction {
        private final Lambda lambda;
        private final Term a;
        private final Term b;
        private final Term c;
        Proxy(Lambda lambda, Term a, Term b, Term c) {
            this.lambda = lambda;
            this.a = a;
            this.b = b;
            this.c = c;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void invoke(Environment environment) {
            LocalContext context = environment.getLocalContext();
            Term aBound = this.a.resolve(context);
            Term bBound = this.b.resolve(context);
            Term cBound = this.c.resolve(context);
            lambda.call(environment, aBound, bBound, cBound);
        }
    }

    /**
     * Create predicate for Lambda instruction
     *
     * @param lambda Lambda instruction
     */
    public BuiltinPredicateArity3(Lambda lambda) {
        this.lambda = lambda;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void compile(Predication predication, CompileContext compiling, CompoundTerm term) {
        Term a = term.get(0);
        Term b = term.get(1);
        Term c = term.get(2);
        compiling.add(term, new Proxy(lambda, a, b, c));
    }
}
