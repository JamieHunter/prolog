// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.exceptions;

import prolog.bootstrap.Interned;
import prolog.constants.PrologAtomLike;
import prolog.constants.PrologString;
import prolog.execution.Environment;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;
import prolog.predicates.Predication;

/**
 * Base class for all Prolog ISO style errors. A Prolog error is a throwable following the pattern of
 * error(Formal, Context). PrologError requires sufficient information to produce the Context {@link ErrorContext}
 * requiring a number of errors to use an intermediate Java-throwable {@link FuturePrologError} that will be
 * caught and transformed into this exception.
 */
public class PrologError extends PrologThrowable {

    /**
     * Convert environment and description into an error context
     *
     * @param environment Environment to extract actual context
     * @param description Description of error
     * @return ErrorContext (compound term)
     */
    public static ErrorContext context(Environment environment, String description) {
        Predication predication = environment.getLocalContext().getPredication();
        return new ErrorContext(predication, description);
    }

    private static String throwableMessage(Throwable throwable) {
        String msg = throwable.getMessage();
        if (msg == null) {
            msg = throwable.toString();
        }
        return msg;
    }

    /**
     * Convert environment and Java throwable into an error context
     *
     * @param environment Environment to extract actual context
     * @param throwable   Exception containing description of error
     * @return ErrorContext (compound term)
     */
    public static ErrorContext context(Environment environment, Throwable throwable) {
        return context(environment, throwableMessage(throwable));
    }

    /**
     * Convert a single atom into a formal argument of error.
     *
     * @param atom Atom with formal error definition
     * @return atom
     */
    public static Term formal(PrologAtomLike atom) {
        return atom;
    }

    /**
     * Convert a functor and a set of arguments into a formal argument of error.
     *
     * @param functor Functor describing formal error definition
     * @param args    Error arguments
     * @return compound formal term
     */
    public static Term formal(PrologAtomLike functor, Term... args) {
        final Term[] copy = args.clone();
        return new CompoundTermImpl(functor, copy);
    }

    /**
     * Construct a generic error term (in general PrologError is subclassed to allow better Java handling of
     * errors).
     *
     * @param formal  Formal description of error
     * @param context Context of error
     * @return PrologError
     */
    private static Term errorTerm(Term formal, ErrorContext context) {
        return new CompoundTermImpl(
                Interned.ERROR_FUNCTOR, formal, context);
    }

    /**
     * Convert a Java exception into a Prolog error.
     *
     * @param throwable Java Exception
     * @return PrologThrowable
     */
    public static PrologThrowable convert(Environment environment, Throwable throwable) {
        if (throwable instanceof PrologThrowable) {
            // these errors can be used as is
            return (PrologThrowable) throwable;
        } else if (throwable instanceof FuturePrologError) {
            // these errors can now be associated with environment related context
            return ((FuturePrologError) throwable).toError(environment);
        } else {
            // in all other cases, give a generic system error
            return systemError(environment, throwable);
        }
    }

    /**
     * Convert an unknown/system Java exception into a Prolog system_error. Format of this error is system defined.
     *
     * @param environment Environment error occurred in for context
     * @param throwable   Java exception
     * @return PrologError
     */
    public static PrologError systemError(Environment environment, Throwable throwable) {
        return new PrologError(
                formal(Interned.SYSTEM_ERROR_FUNCTOR,
                        new PrologString(throwable.getClass().getName())),
                context(environment, throwable),
                throwable);
    }

    /**
     * Base constructor for all Prolog errors
     *
     * @param formal  ISO formal error
     * @param context context(predicator, description)
     * @param cause   Java underlying exception or null
     */
    protected PrologError(Term formal, ErrorContext context, Throwable cause) {
        super(errorTerm(formal, context), context.getMessage(), cause);
    }
}
