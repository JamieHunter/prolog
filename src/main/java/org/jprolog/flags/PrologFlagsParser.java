// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.flags;

import org.jprolog.bootstrap.Interned;
import org.jprolog.constants.Atomic;
import org.jprolog.constants.PrologNumber;
import org.jprolog.exceptions.FutureFlagPermissionError;
import org.jprolog.exceptions.FutureTypeError;
import org.jprolog.expressions.Term;

import java.util.Optional;

/**
 * Class that helps modify a flags object from a list of flags. These structures are expected to be created
 * as part of the bootstrap.
 */
public class PrologFlagsParser extends ReadableParser<PrologFlags.Scope> {

    /**
     * Create a new parser for PrologFlags. This is used to create the global parser.
     */
    PrologFlagsParser() {
    }

    /**
     * Create a copy of a parser, for a new environment. This is a deep copy.
     *
     * @param source Source parser
     */
    PrologFlagsParser(PrologFlagsParser source) {
        super(source);
    }

    /**
     * Create a new key. If a new key returns a constant (read-only), the read/write methods are selected based on that,
     * otherwise the read/write values use a value map in the PrologFlags object (to allow copies).
     *
     * @param key     Flag name
     * @param value   New/initial value
     * @param options Set of options controlling create
     */
    public void create(final PrologFlags obj, final Atomic key, final Term value, final CreateFlagOptions options) {
        ReadableFlagEntry<PrologFlags.Scope> entry = flags.computeIfAbsent(key, ReadableFlagEntry::new);
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
                options.type = Optional.of(CreateFlagOptions.Type.ATOM_integer);
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
            entry.read(o -> o.flags.getOther(key));
            switch (options.type.get()) {
                case ATOM_integer:
                    entry.setOnUpdate((o, v) -> {
                        if (!v.isInteger()) {
                            throw new FutureTypeError(Interned.INTEGER_TYPE, value);
                        }
                        o.flags.setOther(key, v);
                    });
                    break;
                case ATOM_float:
                    entry.setOnUpdate((o, v) -> {
                        if (!v.isNumber()) {
                            throw new FutureTypeError(Interned.NUMBER_TYPE, value);
                        }
                        v = ((PrologNumber) v).toPrologFloat();
                        o.flags.setOther(key, v);
                    });
                    break;
                case ATOM_atom:
                    entry.setOnUpdate((o, v) -> {
                        if (!v.isAtom()) {
                            throw new FutureTypeError(Interned.ATOM_TYPE, value);
                        }
                        o.flags.setOther(key, v);
                    });
                    break;
                default:
                    entry.setOnUpdate((o, v) -> {
                        o.flags.setOther(key, v);
                    });
                    break;
            }
        }
    }
}
