// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.expressions;

import org.jprolog.bootstrap.Interned;
import org.jprolog.enumerators.EnumTermStrategy;
import org.jprolog.exceptions.FutureTypeError;
import org.jprolog.execution.CompileContext;
import org.jprolog.execution.LocalContext;
import org.jprolog.io.WriteContext;

import java.io.IOException;

/**
 * In Prolog, all entities are considered Terms. It is the Prolog equivalent of Object.
 */
public interface Term extends Comparable<Term> {

    /**
     * @param other other term to compare
     * @return true if they are equivalent (sufficiently equal)
     */
    default boolean is(Term other) {
        return this == other || compareTo(other) == 0;
    }

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
     * Returns true if all variables have been instantiated.
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
     * @return unifiable term
     */
    default Term value() {
        return this;
    }

    /**
     * Compile this term as a predicate. This only has meaning for atoms and for compound terms.
     *
     * @param compiling Context used for compilation.
     */
    default void compile(CompileContext compiling) {
        throw new FutureTypeError(Interned.CALLABLE_TYPE, this);
    }

    /**
     * No-op if grounded and not a container. In other cases, it will (a) remove containers, (b) activate labeled
     * variables, (c) replace terms with grounded versions if possible.
     *
     * @param context binding context (used to replace labeled variables with active variables)
     * @return resolved term.
     */
    Term resolve(LocalContext context);

    /**
     * Used to recursively iterate a term performing an operation determined by the strategy class
     *
     * @param strategy Strategy for enumerating a term
     * @return modified term if applicable
     */
    Term enumTerm(EnumTermStrategy strategy);

    /**
     * Render this term per rules and target given in context.
     *
     * @param context Write context
     */
    void write(WriteContext context) throws IOException;

    /**
     * All terms are comparable, and must implement these methods:
     */
    @Override
    default int compareTo(Term o) {
        return compare(this.value(), o.value());
    }

    /**
     * Compare with another term of same type (assumed by rank)
     *
     * @param o Other value
     * @return rank order
     */
    int compareSameType(Term o);

    /**
     * Each Term type is given a rank per Prolog standard. Note, each unique type must be given
     * a unique rank value.
     *
     * @return rank of term type
     */
    int typeRank();

    /**
     * Compare two terms
     *
     * @param left  Left term
     * @param right right term
     * @return 0/-1/1 per {@link #compareTo(Term)}
     */
    static int compare(Term left, Term right) {
        int lt = left.typeRank();
        int rt = right.typeRank();
        if (lt < rt) {
            return -1;
        }
        if (lt > rt) {
            return 1;
        }
        return left.compareSameType(right);
    }

}
