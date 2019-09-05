// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.flags;

import prolog.exceptions.FutureFlagError;
import prolog.exceptions.PrologDomainError;
import prolog.execution.Environment;
import prolog.expressions.Term;

import java.util.Optional;

import static prolog.bootstrap.Interned.internAtom;

/**
 * Structured options parsed from a list of option atoms, used for creating new prolog flags.
 */
public class CreateFlagOptions implements Flags {

    private static OptionParser<CreateFlagOptions> parser = new OptionParser<>();

    static {
        // TODO: These are all placeholders and not yet parsed
        parser.enumFlag(internAtom("access"), Access.class, (o, v) -> o.access = v);
        parser.enumFlag(internAtom("type"), Type.class, (o, v) -> o.type = Optional.of(v));
        parser.booleanFlag(internAtom("keep"), (o, v) -> o.keep = v);
    }

    /**
     * Specify access mode (read_write vs read_only).
     */
    public Access access = Access.ATOM_read_write;
    /**
     * Specify type - this validates writes, ignored if read_only. If not specified,
     * type is inferred from value.
     */
    public Optional<Type> type = Optional.empty();
    /**
     * If true, an existing flag is not overwritten.
     */
    public boolean keep = false;

    /**
     * Set this object of options from a list of option terms.
     *
     * @param environment Execution environment
     * @param optionsTerm List of options
     */
    public CreateFlagOptions(Environment environment, Term optionsTerm) {
        try {
            parser.apply(environment, this, optionsTerm);
        } catch (FutureFlagError ffe) {
            throw PrologDomainError.error(environment, environment.internAtom("create_prolog_flag_option"), ffe.getTerm(), ffe);
        }
    }

    public enum Access {
        ATOM_read_write,
        ATOM_read_only
    }

    public enum Type {
        ATOM_atom,
        ATOM_integer,
        ATOM_float,
        ATOM_term
    }
}
