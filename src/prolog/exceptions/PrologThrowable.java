// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.exceptions;

import prolog.execution.CopyTermContext;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.expressions.Container;
import prolog.expressions.Term;
import prolog.io.WriteContext;

import java.io.IOException;

/**
 * Base class for anything thrown in Prolog. It is considered a container term, and it is also
 * a Java runtime exception. Thus, if not handled by Prolog, it becomes a Java exception. See also
 * {@link FuturePrologError} and {@link PrologError}.
 */
public class PrologThrowable extends RuntimeException implements Container {

    private final Term thrown;

    /**
     * A Java representation of any Prolog thrown exception.
     *
     * @param thrown  Prolog term that was thrown
     * @param message Provides a Java message for the exception
     * @param cause   Provides underlying Java cause if any, or null
     */
    public PrologThrowable(Term thrown, String message, Throwable cause) {
        super(message, cause);
        this.thrown = thrown;
    }

    /**
     * Extract term out of container.
     *
     * @param environment Environment for any environment-specific conversions
     * @return Actual term
     */
    @Override
    public Term value(Environment environment) {
        return thrown.value(environment);
    }

    /**
     * Extract and simplify term.
     *
     * @param environment Environment for value resolving
     * @return Simplified term
     */
    @Override
    public Term simplify(Environment environment) {
        return thrown.simplify(environment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term copyTerm(CopyTermContext context) {
        return context.copy(this, t -> thrown.copyTerm(context));
    }

    /**
     * Extract and resolve term.
     *
     * @param context binding context (used e.g. to create a bound variable)
     * @return Resolved term.
     */
    @Override
    public Term resolve(LocalContext context) {
        return thrown.resolve(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(WriteContext context) throws IOException {
        thrown.write(context);
    }
}
