// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.exceptions;

import org.jprolog.expressions.Term;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.execution.Environment;

/**
 * This will get turned into a PrologDomainError once environment context is known.
 */
public class FutureDomainError extends FuturePrologError {
    private final PrologAtomLike domain;
    private final Term term;

    /**
     * Create a domain exception that is turned into a future PrologDomainError.
     *
     * @param domain Domain identifier
     * @param term   Term in error
     */
    public FutureDomainError(PrologAtomLike domain, Term term) {
        super("Domain error: " + domain.toString());
        this.domain = domain;
        this.term = term;
    }

    /**
     * @return Domain of error
     */
    public PrologAtomLike getDomain() {
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
