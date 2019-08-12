// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.variables.Variable;

import java.util.function.Function;

/**
 * Context for simplifying and unbinding a tree of terms
 */
public class SimplifyTerm extends EnumTermStrategy {

    public SimplifyTerm(Environment environment) {
        super(environment);
    }

    @Override
    public Term visit(Term src, Function<? super Term, ? extends Term> mappingFunction) {
        return copyVisitor(src, mappingFunction);
    }

    public CompoundTerm visit(CompoundTerm compound) {
        return compound.mutateCompoundTerm(this);
    }

    @Override
    public Term visitVariable(Variable variable) {
        return visit(variable, tt -> unbindVariable(variable));
    }

}
