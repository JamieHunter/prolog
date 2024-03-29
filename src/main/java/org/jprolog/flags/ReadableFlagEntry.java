// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.flags;

import org.jprolog.bootstrap.Interned;
import org.jprolog.constants.Atomic;
import org.jprolog.constants.PrologAtom;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.constants.PrologInteger;
import org.jprolog.expressions.Term;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * An extended structure that adds ability to read an object value and to protect the entry.
 */
public class ReadableFlagEntry<T> {
    private final Atomic key;
    private BiConsumer<T, Term> onUpdate;
    private Function<T, Term> onRead;
    private boolean protect = false;

    /**
     * Create a bare entry
     *
     * @param key Flag key
     */
    ReadableFlagEntry(Atomic key) {
        this(key, null);
    }

    /**
     * Create an entry with a consumer to handle updates.
     *
     * @param key      Flag key
     * @param onUpdate update handler
     */
    ReadableFlagEntry(Atomic key, BiConsumer<T, Term> onUpdate) {
        this.key = key;
        this.onUpdate = onUpdate;
        this.onRead = null;
    }

    /**
     * Copy an entry.
     *
     * @param source Source entry
     */
    ReadableFlagEntry(ReadableFlagEntry<T> source) {
        this.key = source.key;
        this.onUpdate = source.onUpdate;
        this.onRead = source.onRead;
        this.protect = source.protect;
    }

    /**
     * Retrieve update handler
     *
     * @return update handler
     */
    BiConsumer<T, Term> getOnUpdate() {
        return onUpdate;
    }

    /**
     * Set/update update handler
     *
     * @param onUpdate update handler
     */
    void setOnUpdate(BiConsumer<T, Term> onUpdate) {
        this.onUpdate = onUpdate;
    }

    /**
     * Retrieve read handler
     *
     * @return Read handler
     */
    Function<T, Term> getOnRead() {
        return onRead;
    }

    /**
     * Set/update read handler
     *
     * @param onRead read handler
     * @return self (for chaining)
     */
    ReadableFlagEntry<T> read(Function<T, Term> onRead) {
        this.onRead = onRead;
        return this;
    }

    /**
     * Set read handler to return constant value
     *
     * @param constValue Constant value
     * @return self (for chaining)
     */
    ReadableFlagEntry<T> constant(Term constValue) {
        return read(o -> constValue);
    }

    /**
     * Set read handler to read integer
     *
     * @param onRead read handler
     * @return self (for chaining)
     */
    ReadableFlagEntry<T> readInteger(Function<T, Long> onRead) {
        return read(o -> PrologInteger.from(onRead.apply(o)));
    }

    /**
     * Set read handler to read boolean atoms
     *
     * @param onRead read handler
     * @return self (for chaining)
     */
    ReadableFlagEntry<T> readBoolean(Function<T, Boolean> onRead) {
        return read(o -> onRead.apply(o) ? Interned.TRUE_ATOM : Interned.FALSE_ATOM);
    }

    /**
     * Set read handler to read boolean atoms described as on/off
     *
     * @param onRead read handler
     * @return self (for chaining)
     */
    ReadableFlagEntry<T> readOnOff(Function<T, Boolean> onRead) {
        return read(o -> onRead.apply(o) ? Interned.ON_ATOM : Interned.OFF_ATOM);
    }

    /**
     * Helper to parse enums. Enum constants have prefix "ATOM_" followed by atom name.
     *
     * @param <E>       Enum class
     * @param enumValue Enum value to convert to atom
     * @return Atom
     */
    private static <E extends Enum<E>> PrologAtomLike parseEnum(E enumValue) {
        String name = enumValue.name();
        if (name.startsWith("ATOM_")) {
            return new PrologAtom(name.substring(5));
        } else {
            throw new InternalError("Expecting prefix ATOM_ on " + name);
        }
    }

    /**
     * Set read handler to read an enum value (from atoms)
     *
     * @param cls    Enum class
     * @param onRead Function to write the enum to the object
     * @param <E>    Enum type
     * @return self (for chaining)
     */
    <E extends Enum<E>> ReadableFlagEntry<T> readEnum(Class<E> cls, Function<T, E> onRead) {
        return read(o -> parseEnum(onRead.apply(o)));
    }

    /**
     * Protect this entry so that it cannot be redefined.
     */
    public ReadableFlagEntry<T> protect() {
        this.protect = true;
        return this;
    }

    /**
     * @return True if entry is protected.
     */
    public boolean isProtected() {
        return protect;
    }
}
