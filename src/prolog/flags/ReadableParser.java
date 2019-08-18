// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.flags;

import prolog.constants.Atomic;
import prolog.exceptions.FutureFlagError;
import prolog.exceptions.FutureFlagKeyError;
import prolog.expressions.Term;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Class that helps configure and access read/write flags object.
 */
class ReadableParser<T extends FlagsWithEnvironment> extends ParserBase<T, ReadableFlagEntry<T>> {
    protected final TreeMap<Atomic, ReadableFlagEntry<T>> flags = new TreeMap<>();

    /**
     * Create a new parser.
     */
    ReadableParser() {
    }

    /**
     * Create a copy of an existing parser with deep copy.
     *
     * @param source Source parser
     */
    ReadableParser(ReadableParser<T> source) {
        for (Map.Entry<Atomic, ReadableFlagEntry<T>> entry : source.flags.entrySet()) {
            flags.put(entry.getKey(), new ReadableFlagEntry<T>(entry.getValue()));
        }
    }

    /**
     * Provides the consumer for flags scenario.
     *
     * @param key Flag key
     * @return consumer
     */
    @Override
    protected BiConsumer<T, Term> getConsumer(Atomic key) {
        ReadableFlagEntry<T> entry = flags.get(key);
        if (entry != null) {
            return entry.getOnUpdate();
        } else {
            return null; // not found
        }
    }

    /**
     * Create a new key entry.
     *
     * @param key      Key name
     * @param onUpdate Function to update value
     * @return entry to allow further modification, see {@link ReadableFlagEntry}.
     */
    @Override
    protected ReadableFlagEntry<T> createKey(Atomic key, BiConsumer<T, Term> onUpdate) {
        ReadableFlagEntry<T> entry = new ReadableFlagEntry<>(key, onUpdate);
        flags.put(key, entry);
        return entry;
    }

    /**
     * Retrieve existing value (even if value is changed through another means)
     *
     * @param obj Instance of PrologFlags
     * @param key Flag name
     * @return Existing value
     */
    public Term get(T obj, Atomic key) {
        ReadableFlagEntry<T> entry = flags.get(key);
        if (entry != null && entry.getOnRead() != null) {
            return entry.getOnRead().apply(obj);
        }
        throw new FutureFlagKeyError(key);
    }

    /**
     * Return all names/values
     *
     * @return collection of all flag names with values
     */
    Map<Atomic, Term> getAll(T obj) {
        TreeMap<Atomic, Term> all = new TreeMap<>();
        for (Map.Entry<Atomic, ReadableFlagEntry<T>> e : flags.entrySet()) {
            try {
                Function<T, Term> func = e.getValue().getOnRead();
                if (func != null) {
                    Term v = func.apply(obj);
                    if (v != null) {
                        all.put(e.getKey(), v);
                    }
                }
            } catch (FutureFlagError ffe) {
                // ignore
            }
        }
        return all;
    }

    /**
     * Change value
     *
     * @param obj   Instance of PrologFlags
     * @param key   Flag name
     * @param value New value
     */
    public void set(T obj, Atomic key, Term value) {
        BiConsumer<T, Term> consumer = getConsumer(key);
        if (consumer != null) {
            consumer.accept(obj, value);
        } else {
            throw new FutureFlagKeyError(key);
        }
    }
}
