// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.exceptions;

import prolog.bootstrap.Interned;
import prolog.constants.PrologAtom;
import prolog.execution.Environment;
import prolog.expressions.Term;

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
    public static PrologDomainError error(Environment environment, PrologAtom domain, Term target, Throwable cause) {
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
    public static PrologDomainError error(Environment environment, PrologAtom domain, Term target) {
        return error(environment, domain, target, null);
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
     * Integer/Float is outside of expected range
     *
     * @param environment Execution environment.
     * @param target      Term with error
     * @return Domain error (not thrown)
     */
    public static PrologDomainError outOfRange(Environment environment, Term target) {
        return error(environment, Interned.OUT_OF_RANGE_DOMAIN, target);
    }

    /**
     * Stream specifier is invalid
     *
     * @param environment Execution environment.
     * @param target      Term with error
     * @return Domain error (not thrown)
     */
    public static PrologDomainError streamOrAlias(Environment environment, Term target) {
        return error(environment, Interned.STREAM_OR_ALIAS_DOMAIN, target);
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
