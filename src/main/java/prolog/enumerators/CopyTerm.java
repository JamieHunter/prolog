// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.enumerators;

import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.variables.Variable;

/**
 * Context for copying a tree of terms
 */
public class CopyTerm extends EnumTermStrategy {

    public CopyTerm(Environment environment) {
        super(environment);
    }

    /**
     * Any variable is replaced with a relabled variable.
     *
     * @param variable Variable reference
     * @return relabeld variable.
     */
    @Override
    public Term visitVariable(Variable variable) {
        return computeUncachedTerm(variable, t -> renameVariable(variable));
    }
}
