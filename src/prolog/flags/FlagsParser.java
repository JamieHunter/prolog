// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.flags;

import prolog.bootstrap.Interned;
import prolog.constants.Atomic;
import prolog.constants.PrologNumber;
import prolog.exceptions.FutureFlagKeyError;
import prolog.exceptions.FutureFlagPermissionError;
import prolog.exceptions.FutureTypeError;
import prolog.execution.Environment;
import prolog.expressions.Term;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Class that helps modify a flags object from a list of flags. These structures are expected to be created
 * as part of the bootstrap.
 */
public class FlagsParser extends ParserBase<PrologFlags, PrologFlagEntry> {
    private final Map<Atomic, PrologFlagEntry> flags = new HashMap<>();

    /**
     * Create a new parser for PrologFlags. This is used to create the global parser.
     */
    FlagsParser() {
    }

    /**
     * Create a copy of a parser, for a new environment. This is a deep copy.
     * @param source Source parser
     */
    FlagsParser(FlagsParser source) {
        for (Map.Entry<Atomic, PrologFlagEntry> entry : flags.entrySet()) {
            flags.put(entry.getKey(), new PrologFlagEntry(entry.getValue()));
        }
    }

    /**
     * Provides the consumer for flags scenario.
     * @param key Flag key
     * @return consumer
     */
    @Override
    protected BiConsumer<PrologFlags, Term> getConsumer(Atomic key) {
        PrologFlagEntry entry = flags.get(key);
        if (entry != null) {
            return entry.getOnUpdate();
        } else {
            return null; // not found
        }
    }

    /**
     * Create a new key entry.
     * @param key Key name
     * @param onUpdate Function to update value
     * @return entry to allow further modification, see {@link PrologFlagEntry}.
     */
    @Override
    protected PrologFlagEntry createKey(Atomic key, BiConsumer<PrologFlags, Term> onUpdate) {
        PrologFlagEntry entry = new PrologFlagEntry(key, onUpdate);
        flags.put(key, entry);
        return entry;
    }

    /**
     * Retrieve existing value (even if value is changed through another means)
     * @param obj Instance of PrologFlags
     * @param key Flag name
     * @return Existing value
     */
    public Term get(PrologFlags obj, Atomic key) {
        PrologFlagEntry entry = flags.get(key);
        if (entry != null && entry.getOnRead() != null) {
            return entry.getOnRead().apply(obj);
        }
        throw new FutureFlagKeyError(key);
    }

    /**
     * Change value
     * @param obj Instance of PrologFlags
     * @param key Flag name
     * @param value New value
     */
    public void set(PrologFlags obj, Atomic key, Term value) {
        BiConsumer<PrologFlags, Term> consumer = getConsumer(key);
        if (consumer != null) {
            consumer.accept(obj, value);
        } else {
            throw new FutureFlagKeyError(key);
        }
    }

    /**
     * Create a new key. If a new key returns a constant (read-only), the read/write methods are selected based on that,
     * otherwise the read/write values use a value map in the PrologFlags object (to allow copies).
     * @param key Flag name
     * @param value New/initial value
     * @param options Set of options controlling create
     */
    public void create(final PrologFlags obj, final Atomic key, final Term value, final CreateFlagOptions options) {
        PrologFlagEntry entry = flags.computeIfAbsent(key, PrologFlagEntry::new);
        if (entry.getOnUpdate() != null && options.keep) {
            // do not modify if it exists
            return;
        }
        if (entry.isProtected()) {
            // cannot modify
            throw new FutureFlagPermissionError(key);
        }
        if (!options.type.isPresent()) {
            // infer from value
            if (value.isInteger()) {
                options.type = Optional.of(CreateFlagOptions.Type.ATOM_iteger);
            } else if (value.isNumber()) {
                options.type = Optional.of(CreateFlagOptions.Type.ATOM_float);
            } else if (value.isAtom()) {
                options.type = Optional.of(CreateFlagOptions.Type.ATOM_atom);
            } else {
                options.type = Optional.of(CreateFlagOptions.Type.ATOM_term);
            }
        }
        if (options.access == CreateFlagOptions.Access.ATOM_read_only) {
            // read-only
            entry.constant(value);
            entry.setOnUpdate((o, v) -> {
                throw new FutureFlagPermissionError(key);
            });
        } else {
            // read/write
            obj.setOther(key, value);
            entry.read(o -> o.getOther(key));
            switch (options.type.get()) {
                case ATOM_iteger:
                    entry.setOnUpdate((o, v) -> {
                        if (!v.isInteger()) {
                            throw new FutureTypeError(Interned.INTEGER_TYPE, value);
                        }
                        o.setOther(key, v);
                    });
                    break;
                case ATOM_float:
                    entry.setOnUpdate((o, v) -> {
                        if (!v.isNumber()) {
                            throw new FutureTypeError(Interned.NUMBER_TYPE, value);
                        }
                        v = ((PrologNumber) v).toPrologFloat();
                        o.setOther(key, v);
                    });
                    break;
                case ATOM_atom:
                    entry.setOnUpdate((o, v) -> {
                        if (!v.isAtom()) {
                            throw new FutureTypeError(Interned.ATOM_TYPE, value);
                        }
                        o.setOther(key, v);
                    });
                    break;
                default:
                    entry.setOnUpdate((o, v) -> {
                        o.setOther(key, v);
                    });
                    break;
            }
        }
    }
}
