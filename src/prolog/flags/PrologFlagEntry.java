// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.flags;

import prolog.bootstrap.Interned;
import prolog.constants.Atomic;
import prolog.constants.PrologAtom;
import prolog.constants.PrologInteger;
import prolog.execution.Environment;
import prolog.expressions.Term;

import java.math.BigInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * For {@link PrologFlags} this adds ability to read an object value and to protect the entry.
 */
public class PrologFlagEntry {
    private final Atomic key;
    private BiConsumer<PrologFlags, Term> onUpdate;
    private Function<PrologFlags, Term> onRead;
    private boolean protect = false;

    /**
     * Create a bare entry
     *
     * @param key Flag key
     */
    PrologFlagEntry(Atomic key) {
        this(key, null);
    }

    /**
     * Create an entry with a consumer to handle updates.
     *
     * @param key      Flag key
     * @param onUpdate update handler
     */
    PrologFlagEntry(Atomic key, BiConsumer<PrologFlags, Term> onUpdate) {
        this.key = key;
        this.onUpdate = onUpdate;
        this.onRead = null;
    }

    /**
     * Copy an entry.
     *
     * @param source Source entry
     */
    PrologFlagEntry(PrologFlagEntry source) {
        this.key = source.key;
        this.onUpdate = source.onUpdate;
        this.onRead = source.onRead;
    }

    /**
     * Retrieve update handler
     *
     * @return update handler
     */
    BiConsumer<PrologFlags, Term> getOnUpdate() {
        return onUpdate;
    }

    /**
     * Set/update update handler
     *
     * @param onUpdate update handler
     */
    void setOnUpdate(BiConsumer<PrologFlags, Term> onUpdate) {
        this.onUpdate = onUpdate;
    }

    /**
     * Retrieve read handler
     *
     * @return Read handler
     */
    Function<PrologFlags, Term> getOnRead() {
        return onRead;
    }

    /**
     * Set/update read handler
     *
     * @param onRead read handler
     * @return self (for chaining)
     */
    PrologFlagEntry read(Function<PrologFlags, Term> onRead) {
        this.onRead = onRead;
        return this;
    }

    /**
     * Set read handler to return constant value
     *
     * @param constValue Constant value
     * @return self (for chaining)
     */
    PrologFlagEntry constant(Term constValue) {
        return read(o -> constValue);
    }

    /**
     * Set read handler to read integer
     *
     * @param onRead read handler
     * @return self (for chaining)
     */
    PrologFlagEntry readInteger(Function<PrologFlags, Integer> onRead) {
        return read(o -> new PrologInteger(BigInteger.valueOf(onRead.apply(o))));
    }

    /**
     * Set read handler to read boolean atoms
     *
     * @param onRead read handler
     * @return self (for chaining)
     */
    PrologFlagEntry readBoolean(Function<PrologFlags, Boolean> onRead) {
        return read(o -> onRead.apply(o) ? Interned.TRUE_ATOM : Interned.FALSE_ATOM);
    }

    /**
     * Helper to parse enums. Enum constants have prefix "ATOM_" followed by atom name.
     *
     * @param environment Execution environment
     * @param enumValue   Enum value to convert to atom
     * @param <E>         Enum class
     * @return Atom
     */
    private static <E extends Enum<E>> PrologAtom parseEnum(Environment environment, E enumValue) {
        String name = enumValue.name();
        if (name.startsWith("ATOM_")) {
            return environment.getAtom(name.substring(5));
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
    <E extends Enum<E>> PrologFlagEntry readEnum(Class<E> cls, Function<PrologFlags, E> onRead) {
        return read(o -> parseEnum(o.environment(), onRead.apply(o)));
    }

    /**
     * Protect this entry so that it cannot be redefined.
     */
    public void protect() {
        this.protect = true;
    }

    /**
     * @return True if entry is protected.
     */
    public boolean isProtected() {
        return protect;
    }
}
