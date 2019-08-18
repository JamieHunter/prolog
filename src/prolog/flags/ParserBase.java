// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.flags;

import prolog.bootstrap.Interned;
import prolog.constants.Atomic;
import prolog.constants.PrologAtomInterned;
import prolog.constants.PrologAtomLike;
import prolog.constants.PrologInteger;
import prolog.exceptions.FutureFlagKeyError;
import prolog.exceptions.FutureFlagPermissionError;
import prolog.exceptions.FutureFlagValueError;
import prolog.exceptions.FutureTypeError;
import prolog.expressions.CompoundTerm;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;
import prolog.expressions.TermList;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Abstract base for flags and options. Allows an update of a Prolog flag to modify a Java class.
 */
public abstract class ParserBase<T extends Flags, R> {

    /**
     * Retrieves a consumer used to update target Flags object.
     *
     * @param key Key (Option/Flag)
     * @return consumer
     */
    protected abstract BiConsumer<T, Term> getConsumer(Atomic key);

    /**
     * Create a new key with a consumer
     *
     * @param key      Option/Flag key
     * @param consumer Consumer function for update
     * @return Option/Flag specific
     */
    protected abstract R createKey(Atomic key, BiConsumer<T, Term> consumer);

    /**
     * Add/change flag in set of flags
     *
     * @param obj  Flags object
     * @param flag Flag to add, specified as a structure
     */
    protected void setFlagFromStruct(T obj, Term flag) {
        if (flag instanceof CompoundTerm) {
            CompoundTerm compoundTerm = (CompoundTerm) flag;
            Atomic key;
            Term value;
            if (CompoundTerm.termIsA(compoundTerm, Interned.EQUALS_FUNCTOR, 2)) {
                // alternative syntax supported by some prolog variants
                // key=value, which is translated to '='(key, value)
                Term keyTerm = compoundTerm.get(0);
                if (!keyTerm.isAtomic()) {
                    throw new FutureFlagKeyError(flag); // report as '='(key,value) as key is not Atomic
                }
                key = (Atomic) keyTerm;
                value = compoundTerm.get(1);
                flag = compoundTerm = new CompoundTermImpl(key, value); // improve error reporting below
            } else {
                if (compoundTerm.arity() != 1) {
                    throw new FutureFlagKeyError(flag);
                }
                key = compoundTerm.functor();
                value = compoundTerm.get(0);
            }
            BiConsumer<T, Term> consumer = getConsumer(key);
            if (consumer == null) {
                // unknown flag
                throw new FutureFlagKeyError(flag);
            }
            consumer.accept(obj, value);
        } else {
            throw new FutureFlagKeyError(flag);
        }
    }

    /**
     * Specify/validate a boolean parameter
     *
     * @param key      Key atom
     * @param consumer Specific value function
     * @return Option/Flag dependent
     */
    public R booleanFlag(final Atomic key, final BiConsumer<T, Boolean> consumer) {
        return createKey(key, (obj, value) -> {
            if (value == Interned.TRUE_ATOM) {
                consumer.accept(obj, true);
            } else if (value == Interned.FALSE_ATOM) {
                consumer.accept(obj, false);
            } else {
                throw new FutureFlagValueError(key, value);
            }
        });
    }

    /**
     * Specify/validate an integer parameter
     *
     * @param key      Key atom
     * @param consumer Specific value function
     * @return Option/Flag dependent
     */
    public R intFlag(final Atomic key, final BiConsumer<T, Long> consumer) {
        return createKey(key, (obj, value) -> {
            if (value.isInteger()) {
                consumer.accept(obj, ((PrologInteger) value).get().longValue());
            } else {
                throw new FutureTypeError(Interned.INTEGER_TYPE, value);
            }
        });
    }

    /**
     * Specify/validate an atom parameter
     *
     * @param key      Key atom
     * @param consumer Specific value function
     * @return Option/Flag dependent
     */
    public R atomFlag(final Atomic key, final BiConsumer<T, PrologAtomLike> consumer) {
        return createKey(key, (obj, value) -> {
            if (value.isAtom()) {
                consumer.accept(obj, (PrologAtomLike) value);
            } else {
                throw new FutureTypeError(Interned.ATOM_TYPE, value);
            }
        });
    }

    /**
     * Map atom value to a strongly typed enum value. The enum values are expected to be of form ATOM_xxxx Where
     * xxxx is the expected atom value.
     *
     * @param cls   Enum class
     * @param key   Flag key (for errors)
     * @param value Flag value (atom)
     * @param <E>   Enum type
     * @return enum constant
     */
    private static <E extends Enum<E>> E toEnum(Class<E> cls, Atomic key, Term value) {
        if (!value.isAtom()) {
            throw new FutureFlagKeyError(key, value);
        }
        String matchName = "ATOM_" + ((PrologAtomLike) value).name();
        try {
            return Enum.valueOf(cls, matchName);
        } catch (IllegalArgumentException ae) {
            throw new FutureFlagValueError(key, value);
        }
    }

    /**
     * Specify/validate an enum parameter.
     *
     * @param key      Key atom
     * @param cls      Enum class
     * @param consumer Specific value function
     * @param <E>      Enum type
     * @return Option/Flag dependent
     */
    public <E extends Enum<E>> R enumFlag(final Atomic key, final Class<E> cls, final BiConsumer<T, E> consumer) {
        return createKey(key, (obj, value) -> {
            E enumValue = toEnum(cls, key, value);
            consumer.accept(obj, enumValue);
        });
    }

    /**
     * Specify/validate a list/set of enums.
     *
     * @param key      Key atom
     * @param cls      Enum class
     * @param consumer Specific value function
     * @param <E>      Enum type
     * @return Option/Flag dependent
     */
    public <E extends Enum<E>> R enumFlags(final Atomic key, final Class<E> cls, final BiConsumer<T, Set<E>> consumer) {
        return createKey(key, (obj, value) -> {
            List<Term> flags = TermList.extractList(value);
            consumer.accept(obj, flags.stream().map(v -> toEnum(cls, key, v)).collect(Collectors.toSet()));
        });
    }

    /**
     * Specify a term that will be used as is.
     *
     * @param key      Key atom
     * @param consumer Specific value function
     * @return Option/Flag dependent
     */
    public R other(final Atomic key, final BiConsumer<T, Term> consumer) {
        return createKey(key, consumer);
    }

    /**
     * Specify a flag that cannot be written.
     *
     * @param key Key atom
     * @return Option/Flag dependent
     */
    public R protectedFlag(final Atomic key) {
        return createKey(key, (obj, value) -> {
            throw new FutureFlagPermissionError(key);
        });
    }

}
