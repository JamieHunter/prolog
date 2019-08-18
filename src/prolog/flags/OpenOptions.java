// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.flags;

import prolog.constants.PrologAtomInterned;
import prolog.constants.PrologAtomLike;
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
        parser.enumFlag(internAtom("type"), StreamProperties.Type.class, (o, v) -> o.type = v);
        parser.booleanFlag(internAtom("bom"), (o, v) -> o.bom = Optional.of(v));
        parser.enumFlag(internAtom("buffer"), StreamProperties.Buffering.class, (o, v) -> o.buffer = v);
        parser.booleanFlag(internAtom("close_on_abort"), (o, v) -> o.closeOnAbort = v);
        parser.enumFlags(internAtom("create"), StreamProperties.Create.class, (o, v) -> o.create = v);
        parser.enumFlag(internAtom("encoding"), StreamProperties.Encoding.class, (o, v) -> o.encoding = Optional.of(v));
        parser.enumFlag(internAtom("eof_action"), StreamProperties.EofAction.class, (o, v) -> o.eofAction = v);
        parser.booleanFlag(internAtom("reposition"), (o, v) -> o.reposition = Optional.of(v));
    }

    /**
     * Specify an alias to use in addition to the file handle
     */
    public Optional<PrologAtomLike> alias = Optional.empty();
    /**
     * Specify text vs binary
     */
    public StreamProperties.Type type = StreamProperties.Type.ATOM_text;
    /**
     * Specify if bom should be checked
     */
    public Optional<Boolean> bom = Optional.empty();
    /**
     * Specify type of buffering
     */
    public StreamProperties.Buffering buffer = StreamProperties.Buffering.ATOM_full;
    /**
     * Specify abort behavior
     */
    public boolean closeOnAbort = true;
    /**
     * Specify create behavior
     */
    public Set<StreamProperties.Create> create = Collections.emptySet();
    /**
     * Specify file encoding, default depends on file type
     */
    public Optional<StreamProperties.Encoding> encoding = Optional.empty();
    /**
     * Specify EOF behavior
     */
    public StreamProperties.EofAction eofAction = StreamProperties.EofAction.ATOM_eof_code;
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
            throw PrologDomainError.streamOption(environment, ffe.getTerm());
        }
    }

}
