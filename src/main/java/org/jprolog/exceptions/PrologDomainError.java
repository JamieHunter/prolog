// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.exceptions;

import org.jprolog.bootstrap.Interned;
import org.jprolog.constants.PrologAtom;
import org.jprolog.constants.PrologInteger;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.CompoundTermImpl;
import org.jprolog.expressions.Term;

/**
 * Prolog Domain error. A domain error occurs when a value is of the correct type, but outside of the expected domain
 * of values of that type.
 */
public class PrologDomainError extends PrologError {

    /**
     * Create a domain error, generic form.
     *
     * @param environment Execution environment.
     * @param domain      Domain description as an atom
     * @param target      Term that has the error
     * @param cause       Exception that lead to this error
     * @return Domain error (not thrown)
     */
    public static PrologDomainError error(Environment environment, Term domain, Term target, Throwable cause) {
        return new PrologDomainError(
                formal(Interned.DOMAIN_ERROR_FUNCTOR, domain, target),
                context(environment, "Domain error: " + domain.toString()),
                cause);
    }

    /**
     * Simplified error form
     *
     * @param environment Execution environment.
     * @param domain      Domain in error
     * @param target      Term that has the error
     * @return Domain error (not thrown)
     */
    public static PrologDomainError error(Environment environment, Term domain, Term target) {
        return error(environment, domain, target, null);
    }

    /**
     * Simplified error form, domain can be a string
     *
     * @param environment Execution environment.
     * @param domain      Domain in error
     * @param target      Term that has the error
     * @return Domain error (not thrown)
     */
    public static PrologDomainError error(Environment environment, String domain, Term target) {
        return error(environment, new PrologAtom(domain), target);
    }

    /**
     * Domain error completion of {@link FutureDomainError}.
     *
     * @param environment Execution environment.
     * @param cause       FutureDomainError exception
     * @return Domain error (not thrown)
     */
    public static PrologDomainError error(Environment environment, FutureDomainError cause) {
        return error(environment, cause.getDomain(), cause.getTerm(), cause);
    }

    /**
     * Integer expected to be 0 or more
     *
     * @param environment Execution environment.
     * @param target      Term with error
     * @return Domain error (not thrown)
     */
    public static PrologDomainError notLessThanZero(Environment environment, Term target) {
        return error(environment, Interned.NOT_LESS_THAN_ZERO_DOMAIN, target);
    }

    /**
     * List is expected to not be empty
     *
     * @param environment Execution environment.
     * @param target      Term with error
     * @return Domain error (not thrown)
     */
    public static PrologDomainError nonEmptyList(Environment environment, Term target) {
        return error(environment, Interned.NON_EMPTY_LIST_DOMAIN, target);
    }

    /**
     * Integer/Float is outside of expected range
     *
     * @param environment Execution environment.
     * @param min         Minimum value
     * @param max         Maximum value
     * @param target      Term with error
     * @return Domain error (not thrown)
     */
    public static PrologDomainError range(Environment environment, long min, long max, Term target) {
        return error(environment,
                new CompoundTermImpl(Interned.RANGE_DOMAIN,
                        PrologInteger.from(min), PrologInteger.from(max)), target);
    }

    /**
     * Stream specifier is invalid (alias not allowed)
     *
     * @param environment Execution environment.
     * @param target      Term with error
     * @return Domain error (not thrown)
     */
    public static PrologDomainError stream(Environment environment, Term target) {
        return error(environment, Interned.STREAM_DOMAIN, target);
    }

    /**
     * Stream specifier is invalid (alias is allowed)
     *
     * @param environment Execution environment.
     * @param target      Term with error
     * @return Domain error (not thrown)
     */
    public static PrologDomainError streamOrAlias(Environment environment, Term target) {
        return error(environment, Interned.STREAM_OR_ALIAS_DOMAIN, target);
    }

    /**
     * Stream property is invalid
     *
     * @param environment Execution environment.
     * @param target      Term with error
     * @return Domain error (not thrown)
     */
    public static PrologDomainError streamProperty(Environment environment, Term target) {
        return error(environment, Interned.STREAM_PROPERTY_DOMAIN, target);
    }

    /**
     * Stream file name is invalid
     *
     * @param environment Execution environment.
     * @param target      Term with error
     * @return Domain error (not thrown)
     */
    public static PrologDomainError sourceSink(Environment environment, Term target) {
        return error(environment, Interned.SOURCE_SINK_DOMAIN, target);
    }

    /**
     * IO Mode is invalid
     *
     * @param environment Execution environment
     * @param target      Term with error
     * @return Domain error (not thrown)
     */
    public static PrologDomainError ioMode(Environment environment, Term target) {
        return error(environment, Interned.IO_MODE_DOMAIN, target);
    }

    /**
     * Operator priority atom is invalid
     *
     * @param environment Execution environment
     * @param target      Term with error
     * @return Domain error (not thrown)
     */
    public static PrologDomainError operatorPriority(Environment environment, Term target) {
        return error(environment, Interned.OPERATOR_PRIORITY_DOMAIN, target);
    }

    /**
     * Invalid read option
     *
     * @param environment Execution environment
     * @param target      Option with error
     * @return Domain error (not thrown)
     */
    public static PrologDomainError readOption(Environment environment, Term target) {
        return error(environment, new PrologAtom("read_option"), target);
    }

    /**
     * Invalid stream option
     *
     * @param environment Execution environment
     * @param target      Option with error
     * @return Domain error (not thrown)
     */
    public static PrologDomainError streamOption(Environment environment, Term target) {
        return error(environment, new PrologAtom("stream_option"), target);
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
    protected PrologDomainError(Term formal, ErrorContext context, Throwable cause) {
        super(formal, context, cause);
    }

}
