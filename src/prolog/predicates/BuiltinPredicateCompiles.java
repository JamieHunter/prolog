// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.predicates;

import prolog.execution.CompileContext;
import prolog.expressions.CompoundTerm;

/**
 * A predicate that is built in to Prolog that compiles a simple instruction per specified
 * method. Used for compile-style library methods.
 */
public class BuiltinPredicateCompiles extends BuiltInPredicate {
    public static final String METHOD_NAME = "call"; // name of method exposed by Compiles interface
    private final Compiles lambda;

    @FunctionalInterface
    public interface Compiles {
        void call(CompileContext context, CompoundTerm term);
    }

    /**
     * Construct predicate with a lambda function to use to compile instructions.
     */
    public BuiltinPredicateCompiles(Compiles lambda) {
        this.lambda = lambda;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void compile(Predication predication, CompileContext compiling, CompoundTerm term) {
        lambda.call(compiling, term);
    }
}
