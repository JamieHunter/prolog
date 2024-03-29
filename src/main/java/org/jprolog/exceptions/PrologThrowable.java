// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.exceptions;

import org.jprolog.enumerators.EnumTermStrategy;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.Container;
import org.jprolog.expressions.Term;
import org.jprolog.io.WriteContext;

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
     * {@inheritDoc}
     */
    @Override
    public Term value() {
        return thrown.value();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term enumTerm(EnumTermStrategy strategy) {
        return strategy.visitContainer(this);
    }

    /**
     * Extract and resolve term.
     *
     * @param context binding context (used e.g. to create an active variable)
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
