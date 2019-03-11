// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.exceptions;

import prolog.constants.PrologAtom;
import prolog.execution.Environment;
import prolog.expressions.Term;

/**
 * This will get turned into a PrologDomainError once environment context is known.
 */
public class FutureDomainError extends FuturePrologError {
    private final PrologAtom domain;
    private final Term term;

    /**
     * Create a domain exception that is turned into a future PrologDomainError.
     *
     * @param domain Domain identifier
     * @param term   Term in error
     */
    public FutureDomainError(PrologAtom domain, Term term) {
        super("Domain error: " + domain.toString());
        this.domain = domain;
        this.term = term;
    }

    /**
     * @return Domain of error
     */
    public PrologAtom getDomain() {
        return domain;
    }

    /**
     * @return Term that has the error
     */
    public Term getTerm() {
        return term;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologDomainError toError(Environment environment) {
        return PrologDomainError.error(environment, this);
    }
}
