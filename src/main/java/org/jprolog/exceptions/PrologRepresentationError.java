// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.exceptions;

import org.jprolog.bootstrap.Interned;
import org.jprolog.constants.PrologAtom;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.Term;

/**
 * Prolog Representation error. A representation error occurs when a value cannot be represented correctly.
 */
public class PrologRepresentationError extends PrologError {

    /**
     * Create a representation error, generic form.
     *
     * @param environment    Execution environment.
     * @param representation Representation in error
     * @param cause          Exception that lead to this error
     * @return Representation error (not thrown)
     */
    public static PrologRepresentationError error(Environment environment, PrologAtomLike representation, Throwable cause) {
        return new PrologRepresentationError(
                formal(Interned.REPRESENTATION_ERROR_FUNCTOR, representation),
                context(environment, "Representation error: " + representation.toString()),
                cause);
    }

    /**
     * Simplified error form
     *
     * @param environment    Execution environment.
     * @param representation Representation in error
     * @return Representation error (not thrown)
     */
    public static PrologRepresentationError error(Environment environment, PrologAtomLike representation) {
        return error(environment, representation, null);
    }

    /**
     * Simplified error form, representation can be a string
     *
     * @param environment    Execution environment.
     * @param representation Representation in error
     * @return Representation error (not thrown)
     */
    public static PrologRepresentationError error(Environment environment, String representation) {
        return error(environment, new PrologAtom(representation));
    }

    /**
     * Representation error completion of {@link FutureRepresentationError}.
     *
     * @param environment Execution environment.
     * @param cause       FutureRepresentationError exception
     * @return Representation error (not thrown)
     */
    public static PrologRepresentationError error(Environment environment, FutureRepresentationError cause) {
        return error(environment, cause.getRepresentation(), cause);
    }

    /**
     * Create a Java error representation of a Prolog error(formal,context) exception.
     * In general, any Java thrown exceptions are converted to this to allow dual
     * representation between Java and Prolog.
     *
     * @param formal  ISO formal error
     * @param context context(predicator, description)
     * @param cause   Java underlying exception or null
     */
    private PrologRepresentationError(Term formal, ErrorContext context, Throwable cause) {
        super(formal, context, cause);
    }

}
