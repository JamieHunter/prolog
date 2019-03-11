// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.predicates;

import prolog.exceptions.PrologTypeError;
import prolog.execution.CompileContext;
import prolog.expressions.CompoundTerm;

/**
 * Handles the predicate definition
 */
public abstract class PredicateDefinition {

    /**
     * Update compilation block
     * @param compiling Compilation context
     *
     */
    public void compile(Predication predication, CompileContext compiling, CompoundTerm term) {
        throw PrologTypeError.callableExpected(compiling.environment(), term);
    }

    /**
     * @return true if dynamic inserts allowed.
     */
    abstract public boolean isDynamic();
}
