// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.predicates;

import org.jprolog.execution.CompileContext;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.functions.CompileMathExpression;
import org.jprolog.functions.StackFunction;
import org.jprolog.instructions.ExecPopAndTest;

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
        compiling.add(source, new ExecPopAndTest(expr));
    }
}
