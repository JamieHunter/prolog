// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

import prolog.expressions.Term;
import prolog.variables.Variable;

/**
 * Context for simplifying and unbinding a tree of terms
 */
public class SimplifyTerm extends EnumTermStrategy {

    public SimplifyTerm(Environment environment) {
        super(environment);
    }

    /**
     * Variables are replaced with an unbound version, performed with caching.
     *
     * @param variable Variable reference
     * @return unbound variable.
     */
    @Override
    public Term visitVariable(Variable variable) {
        return this.computeUncachedTerm(variable, t -> unbindVariable(variable));
    }

}
