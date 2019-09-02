// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.predicates;

import prolog.execution.CompileContext;
import prolog.expressions.CompoundTerm;
import prolog.functions.StackFunction;
import prolog.instructions.*;
import prolog.functions.CompileMathExpression;

/**
 * Predicate for comparing two expressions.
 */
public class BuiltinPredicateCompare extends BuiltInPredicate {

    private final StackFunction function;

    /**
     * Construct predicate with a stack function to use to perform the compare.
     */
    public BuiltinPredicateCompare(StackFunction function) {
        this.function = function;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void compile(Predication predication, CompileContext compiling, CompoundTerm source) {
        CompileMathExpression expr = new CompileMathExpression(compiling).compileFunction(source, function);
        compiling.add(new ExecPopAndTest(source, expr));
    }
}
