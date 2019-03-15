// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.flags;

import prolog.constants.Atomic;
import prolog.exceptions.FutureFlagError;
import prolog.exceptions.PrologDomainError;
import prolog.execution.Environment;
import prolog.expressions.Term;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static prolog.bootstrap.Interned.internAtom;

/**
 * Structured options parsed from a list of option atoms, used for opening files.
 */
public class OpenOptions implements Flags {

    private static OptionParser<OpenOptions> parser = new OptionParser<>();

    static {
        // TODO: These are all placeholders and not yet parsed
        parser.atomFlag(internAtom("alias"), (o, v) -> o.alias = Optional.of(v));
        parser.enumFlag(internAtom("type"), Type.class, (o, v) -> o.type = v);
        parser.booleanFlag(internAtom("bom"), (o, v) -> o.bom = Optional.of(v));
        parser.enumFlag(internAtom("buffer"), Buffering.class, (o, v) -> o.buffer = v);
        parser.booleanFlag(internAtom("close_on_abort"), (o, v) -> o.closeOnAbort = v);
        parser.enumFlags(internAtom("create"), Create.class, (o, v) -> o.create = v);
        parser.enumFlag(internAtom("encoding"), Encoding.class, (o, v) -> o.encoding = Optional.of(v));
        parser.enumFlag(internAtom("eof_action"), EofAction.class, (o, v) -> o.eofAction = v);
        parser.booleanFlag(internAtom("reposition"), (o, v) -> o.reposition = Optional.of(v));
    }

    /**
     * Specify an alias to use in addition to the file handle
     */
    public Optional<Atomic> alias = Optional.empty();
    /**
     * Specify text vs binary
     */
    public Type type = Type.ATOM_text;
    /**
     * Specify if bom should be checked
     */
    public Optional<Boolean> bom = Optional.empty();
    /**
     * Specify type of buffering
     */
    public Buffering buffer = Buffering.ATOM_full;
    /**
     * Specify abort behavior
     */
    public boolean closeOnAbort = true;
    /**
     * Specify create behavior
     */
    public Set<Create> create = Collections.emptySet();
    /**
     * Specify file encoding, default depends on file type
     */
    public Optional<Encoding> encoding = Optional.empty();
    /**
     * Specify EOF behavior
     */
    public EofAction eofAction = EofAction.ATOM_eof_code;
    /**
     * Specify if respositioning is requested
     */
    public Optional<Boolean> reposition = Optional.empty();

    /**
     * Set this object of options from a list of option terms.
     *
     * @param environment Execution environment
     * @param optionsTerm List of options
     */
    public OpenOptions(Environment environment, Term optionsTerm) {
        try {
            parser.apply(environment, this, optionsTerm);
        } catch (FutureFlagError ffe) {
            throw PrologDomainError.error(environment, environment.getAtom("open_option"), ffe.getTerm(), ffe);
        }
    }

    public enum Buffering {
        ATOM_full,
        ATOM_line,
        ATOM_false
    }

    private enum Create {
        ATOM_read,
        ATOM_write,
        ATOM_execute,
        ATOM_default,
        ATOM_all
    }

    private enum Encoding {
        ATOM_utf8,
        ATOM_octet,
        ATOM_ascii,
        ATOM_iso_latin_1,
        ATOM_text,
        ATOM_unicode_be,
        ATOM_unicode_le
    }

    private enum EofAction {
        ATOM_eof_code,
        ATOM_error,
        ATOM_reset
    }

    private enum Type {
        ATOM_text,
        ATOM_binary
    }
}
