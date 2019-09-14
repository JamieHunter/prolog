// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.exceptions;

import org.jprolog.constants.PrologAtomLike;
import org.jprolog.execution.Environment;

/**
 * This will get turned into a PrologRepresentationError once environment context is known.
 */
public class FutureRepresentationError extends FuturePrologError {
    private final PrologAtomLike representation;

    /**
     * Create a representation exception that is turned into a future PrologRepresentationError.
     *
     * @param representation Representation identifier
     */
    public FutureRepresentationError(PrologAtomLike representation) {
        super("Representation error: " + representation.toString());
        this.representation = representation;
    }

    /**
     * @return Representation identifier
     */
    public PrologAtomLike getRepresentation() {
        return representation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologRepresentationError toError(Environment environment) {
        return PrologRepresentationError.error(environment, this);
    }
}
