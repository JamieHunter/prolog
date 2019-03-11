// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.exceptions;

import prolog.constants.PrologAtom;
import prolog.execution.Environment;
import prolog.expressions.Term;

/**
 * This will get turned into a PrologTypeError once context is known
 */
public class FutureTypeError extends FuturePrologError {
    private final PrologAtom type;
    private final Term term;

    /**
     * Construct a FutureTypeError
     *
     * @param type Atom providing describing type (usually expected type).
     * @param term Term that did not match expected type.
     */
    public FutureTypeError(PrologAtom type, Term term) {
        super("Type expected to be: " + type.toString());
        this.type = type;
        this.term = term;
    }

    /**
     * @return type name
     */
    public PrologAtom getType() {
        return type;
    }

    /**
     * @return term in error
     */
    public Term getTerm() {
        return term;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologTypeError toError(Environment environment) {
        return PrologTypeError.error(environment, this);
    }
}
