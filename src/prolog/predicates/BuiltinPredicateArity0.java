// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.predicates;

import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.LocalContext;
import prolog.expressions.CompoundTerm;

/**
 * A predicate that is built in to Prolog that compiles a simple instruction with no terms
 */
public class BuiltinPredicateArity0 extends BuiltInPredicate {
    private final Lambda lambda;

    /**
     * Lambda of arity 0
     */
    @FunctionalInterface
    public interface Lambda {
        void call(Environment environment);
    }

    /**
     * Proxy around lambda to invoke lambda in response to instruction.
     */
    private static class Proxy implements Instruction {
        private final Lambda lambda;

        Proxy(Lambda lambda) {
            this.lambda = lambda;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void invoke(Environment environment) {
            LocalContext context = environment.getLocalContext();
            lambda.call(environment);
        }
    }

    /**
     * Construct specifying lambda function
     *
     * @param lambda Lambda singleton function
     */
    public BuiltinPredicateArity0(Lambda lambda) {
        this.lambda = lambda;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void compile(Predication predication, CompileContext compiling, CompoundTerm term) {
        compiling.add(new Proxy(lambda));
    }
}
