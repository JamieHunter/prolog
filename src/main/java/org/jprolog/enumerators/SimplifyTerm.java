// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.enumerators;

import org.jprolog.execution.Environment;
import org.jprolog.expressions.Term;
import org.jprolog.variables.Variable;

/**
 * Context for simplifying a tree of terms, in particular, replacing variables with labels.
 */
public class SimplifyTerm extends EnumTermStrategy {

    public SimplifyTerm(Environment environment) {
        super(environment);
    }

    /**
     * Variables are replaced with an inactive labeled version, performed with caching.
     *
     * @param variable Variable reference
     * @return inactive variable.
     */
    @Override
    public Term visitVariable(Variable variable) {
        return this.computeUncachedTerm(variable, t -> variable.label());
    }

}
