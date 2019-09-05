// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.predicates;

import prolog.bootstrap.Interned;
import prolog.exceptions.FutureTypeError;
import prolog.execution.CompileContext;
import prolog.expressions.CompoundTerm;

/**
 * Handles the predicate definition
 */
public abstract class PredicateDefinition {

    /**
     * Update compilation block
     *
     * @param compiling Compilation context
     */
    public void compile(Predication predication, CompileContext compiling, CompoundTerm term) {
        throw new FutureTypeError(Interned.CALLABLE_TYPE, term);
    }

    /**
     * Predicate that is considered for current_predicate()
     * @return true if predicate is user-defined and contains at least one clause.
     */
    public boolean isCurrentPredicate() {
        return false;
    }

    /**
     * @return true if dynamic inserts allowed.
     */
    public abstract boolean isDynamic();

    /**
     * @return true if multifile behavior enabled.
     */
    public abstract boolean isMultifile();

    /**
     * @return true if discontiguous behavior enabled.
     */
    public abstract boolean isDiscontiguous();

}
