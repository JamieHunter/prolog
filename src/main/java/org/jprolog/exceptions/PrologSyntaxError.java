// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.exceptions;

import org.jprolog.expressions.Term;
import org.jprolog.bootstrap.Interned;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.execution.Environment;

/**
 * Prolog Syntax error. A syntax error may occur during read of a Prolog sentence. The type of syntax error is
 * system defined.
 */
public class PrologSyntaxError extends PrologError {

    /**
     * Create a syntax error, providing atom identifier for type.
     *
     * @param environment Execution environment
     * @param type        Type as an atom (system defined)
     * @param message     Display message
     * @return exception (not thrown)
     */
    public static PrologSyntaxError error(Environment environment, PrologAtomLike type, String message) {
        return new PrologSyntaxError(
                formal(Interned.SYNTAX_ERROR_FUNCTOR, type),
                context(environment, message),
                null);
    }

    /**
     * Create a syntax error, providing string identifier for type.
     *
     * @param environment Execution environment
     * @param type        Type as a string (converted to an atom)
     * @param message     Display message
     * @return exception (not thrown)
     */
    public static PrologSyntaxError error(Environment environment, String type, String message) {
        return error(environment, environment.internAtom(type), message);
    }

    /**
     * Token is not understood
     *
     * @param environment Execution environment
     * @param message     Error message
     * @return exception (not thrown)
     */
    public static PrologSyntaxError tokenError(Environment environment, String message) {
        return error(environment, "token_error", message);
    }

    /**
     * A string is being parsed, but the string is badly formed.
     *
     * @param environment Execution environment
     * @param message     Error message
     * @return exception (not thrown)
     */
    public static PrologSyntaxError stringError(Environment environment, String message) {
        return error(environment, "string_error", message);
    }

    /**
     * A comment is being parsed, but the comment is badly formed.
     *
     * @param environment Execution environment
     * @param message     Error message
     * @return exception (not thrown)
     */
    public static PrologSyntaxError commentError(Environment environment, String message) {
        return error(environment, "comment_error", message);
    }

    /**
     * A token is expected (e.g. ')') but end of input/file is reached.
     *
     * @param environment Execution environment
     * @param message     Error message
     * @return exception (not thrown)
     */
    public static PrologSyntaxError eofError(Environment environment, String message) {
        return error(environment, "end_of_file_error", message);
    }

    /**
     * Attempting to parse a compound term, however token is illegal. Cause could be due to missing operator.
     *
     * @param environment Execution environment
     * @param message     Error message
     * @return exception (not thrown)
     */
    public static PrologSyntaxError functorError(Environment environment, String message) {
        return error(environment, "functor_or_operator_error", message);
    }

    /**
     * A binary/suffix operator was expected, but the token parsed was not an operator.
     *
     * @param environment Execution environment
     * @param message     Error message
     * @return exception (not thrown)
     */
    public static PrologSyntaxError expectedOperatorError(Environment environment, String message) {
        return error(environment, "expected_operator_error", message);
    }

    /**
     * An argument was expected (e.g. right of a binary operator), but the token cannot be interpreted as an argument.
     *
     * @param environment Execution environment
     * @param message     Error message
     * @return exception (not thrown)
     */
    public static PrologSyntaxError expectedArgumentError(Environment environment, String message) {
        return error(environment, "expected_argument_error", message);
    }

    /**
     * A sentence was expected, however a single period was given
     *
     * @param environment Execution environment
     * @param message     Error message
     * @return exception (not thrown)
     */
    public static PrologSyntaxError expectedSentenceError(Environment environment, String message) {
        return error(environment, "expected_sentence_error", message);
    }

    /**
     * {@inheritDoc}
     */
    protected PrologSyntaxError(Term formal, ErrorContext context, Throwable cause) {
        super(formal, context, cause);
    }

}
