// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.exceptions;

import prolog.bootstrap.Interned;
import prolog.constants.PrologAtomLike;
import prolog.execution.Environment;
import prolog.expressions.Term;

/**
 * Prolog Type error. A type error occurs when a value is of the wrong type, e.g. Float instead of Integer.
 */
public class PrologTypeError extends PrologError {

    /**
     * Create a type error, generic form.
     *
     * @param environment Execution environment.
     * @param type        Atom name of type
     * @param target      Term that has the error
     * @param cause       Java exception that lead to this error
     * @return Type error (not thrown)
     */
    public static PrologTypeError error(Environment environment, PrologAtomLike type, Term target, Throwable cause) {
        return new PrologTypeError(
                formal(Interned.TYPE_ERROR_FUNCTOR, type, target),
                context(environment, "Type expected to be: " + type.toString()),
                cause);
    }

    /**
     * Create a type error, more specific form.
     *
     * @param environment Execution environment.
     * @param type        Atom name of type
     * @param target      Term that has the error
     * @return Type error (not thrown)
     */
    public static PrologTypeError error(Environment environment, PrologAtomLike type, Term target) {
        return error(environment, type, target, null);
    }

    /**
     * Type error completion of {@link FutureTypeError}.
     *
     * @param environment Execution environment.
     * @param cause       FutureTypeError exception
     * @return Type error (not thrown)
     */
    public static PrologTypeError error(Environment environment, FutureTypeError cause) {
        return error(environment, cause.getType(), cause.getTerm(), cause);
    }

    /**
     * Standard type error - integer expected.
     *
     * @param environment Execution environment.
     * @param target      Term that has the error
     * @return Type error (not thrown)
     */
    public static PrologTypeError integerExpected(Environment environment, Term target) {
        return error(environment, Interned.INTEGER_TYPE, target);
    }

    /**
     * Standard type error - atom expected.
     *
     * @param environment Execution environment.
     * @param target      Term that has the error
     * @return Type error (not thrown)
     */
    public static PrologTypeError atomExpected(Environment environment, Term target) {
        return error(environment, Interned.ATOM_TYPE, target);
    }

    /**
     * Standard type error - atomic expected.
     *
     * @param environment Execution environment.
     * @param target      Term that has the error
     * @return Type error (not thrown)
     */
    public static PrologTypeError atomicExpected(Environment environment, Term target) {
        return error(environment, Interned.ATOMIC_TYPE, target);
    }

    /**
     * Standard type error - number (integer or float) expected
     *
     * @param environment Execution environment.
     * @param target      Term that has the error
     * @return Type error (not thrown)
     */
    public static PrologTypeError numberExpected(Environment environment, Term target) {
        return error(environment, Interned.NUMBER_TYPE, target);
    }

    /**
     * Standard type error - character (single character atom) expected
     *
     * @param environment Execution environment.
     * @param target      Term that has the error
     * @return Type error (not thrown)
     */
    public static PrologTypeError characterExpected(Environment environment, Term target) {
        return error(environment, Interned.CHARACTER_TYPE, target);
    }

    /**
     * Standard type error - list expected.
     *
     * @param environment Execution environment.
     * @param target      Term that has the error
     * @return Type error (not thrown)
     */
    public static PrologTypeError listExpected(Environment environment, Term target) {
        return error(environment, Interned.LIST_TYPE, target);
    }

    /**
     * Standard type error - callable (atom or compound) expected.
     *
     * @param environment Execution environment.
     * @param target      Term that has the error
     * @return Type error (not thrown)
     */
    public static PrologTypeError callableExpected(Environment environment, Term target) {
        return error(environment, Interned.CALLABLE_TYPE, target);
    }

    /**
     * Standard type error - evaluable (atom or compound) expected.
     *
     * @param environment Execution environment.
     * @param target      Term that has the error
     * @return Type error (not thrown)
     */
    public static PrologTypeError evaluableExpected(Environment environment, Term target) {
        return error(environment, Interned.EVALUABLE_TYPE, target);
    }

    /**
     * Standard type error - predicate indicator expected
     *
     * @param environment Execution environment.
     * @param target      Term that has the error
     * @return Type error (not thrown)
     */
    public static PrologTypeError predicateIndicatorExpected(Environment environment, Term target) {
        return error(environment, Interned.PREDICATE_INDICATOR_TYPE, target);
    }

    /**
     * Standard type error - compound expected.
     *
     * @param environment Execution environment.
     * @param target      Term that has the error
     * @return Type error (not thrown)
     */
    public static PrologTypeError compoundExpected(Environment environment, Term target) {
        return error(environment, Interned.COMPOUND_TYPE, target);
    }

    /**
     * {@inheritDoc}
     */
    protected PrologTypeError(Term formal, ErrorContext context, Throwable cause) {
        super(formal, context, cause);
    }

}
