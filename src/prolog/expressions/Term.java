// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.expressions;

import prolog.exceptions.PrologTypeError;
import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.io.WriteContext;

import java.io.IOException;

/**
 * In Prolog, all entities are considered Terms. It is the Prolog equivalent of Object.
 */
public interface Term {

    /**
     * @return true if this term is considered an atom. By default, terms are not atoms.
     */
    default boolean isAtom() {
        return false;
    }

    /**
     * @return true if this term is considered atomic.
     */
    default boolean isAtomic() {
        return false;
    }

    /**
     * @return true if Atomic and integer
     */
    default boolean isInteger() {
        return false;
    }

    /**
     * @return true if Atomic and floating point
     */
    default boolean isFloat() {
        return false;
    }

    /**
     * @return true if Atomic and a number
     */
    default boolean isNumber() {
        return false;
    }

    /**
     * @return true if Atomic and a String
     */
    default boolean isString() {
        return false;
    }

    /**
     * Returns true if all variables have been bound and instantiated.
     *
     * @return true if grounded
     */
    default boolean isGrounded() {
        return false;
    }

    /**
     * Returns true if not a variable, or if variable is instantiated. Note that a term
     * can be instantiated but not resolved if it is a structured term.
     *
     * @return true if instantiated
     */
    default boolean isInstantiated() {
        return true;
    }

    /**
     * Attempt to instantiate, if this term is a variable. The return value of true indicates that
     * unification has been implicitly performed through instantiation.
     *
     * @param value Value to instantiate to
     * @return true if instantiated, false if instantiation was not performed and unification comparison is needed.
     */
    default boolean instantiate(Term value) {
        return false;
    }

    /**
     * Convert this term into a value suitable for unification or instantiation. Container terms require value to
     * be called to decontainerize the value.
     *
     * @param environment Environment for any environment-specific conversions
     * @return unifiable term
     */
    default Term value(Environment environment) {
        return this;
    }

    /**
     * Compile this term as a predicate. This only has meaning for atoms and for compound terms.
     *
     * @param compiling Context used for compilation.
     */
    default void compile(CompileContext compiling) {
        throw PrologTypeError.callableExpected(compiling.environment(), this);
    }

    /**
     * No-op if grounded and not a container. In other cases, attempts to convert the term into a grounded value.
     *
     * @param context binding context (used e.g. to create a bound variable)
     * @return resolved term, after replacing all instantiated variables and values.
     */
    Term resolve(LocalContext context);

    /**
     * Simplify a structure. This is similar to resolve, but replaces uninstantiated variables with
     * a placeholder that is not bound to a local context. Effectively undoing BoundedVariable. During parsing it
     * also removes intermediate containers that help provide parsing context.
     *
     * @param environment Environment for value resolving
     * @return simplified term
     */
    default Term simplify(Environment environment) {
        return value(environment);
    }

    /**
     * Render this term per rules and target given in context.
     *
     * @param context Write context
     */
    void write(WriteContext context) throws IOException;

}
