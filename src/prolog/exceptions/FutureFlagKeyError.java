// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.exceptions;

import prolog.constants.Atomic;
import prolog.execution.Environment;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;

/**
 * Prolog Option/Flag error. An option error occurs when an option is invalid. Normally this is caught and converted to a
 * more specific domain error.
 */
public class FutureFlagKeyError extends FutureFlagError {
    private final Term term;

    /**
     * Create a domain exception that is turned into a future PrologDomainError.
     *
     * @param term   Term in error
     */
    public FutureFlagKeyError(Term term) {
        super("Domain option/flag unrecognized: " + term.toString());
        this.term = term;
    }

    /**
     * Create a domain exception that is turned into a future PrologDomainError.
     *
     * @param key    Flag key
     * @param value  Flag value
     */
    public FutureFlagKeyError(Atomic key, Term value) {
        this(new CompoundTermImpl(key, value));
    }

    /**
     * @return Term that has the error
     */
    @Override
    public Term getTerm() {
        return term;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologDomainError toError(Environment environment) {
        return PrologDomainError.error(environment, environment.getAtom("prolog_flag"), term, this);
    }
}
