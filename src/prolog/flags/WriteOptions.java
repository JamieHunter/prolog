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
 * Structured options parsed from a list of option atoms. Options used for write formatting.
 */
public class WriteOptions implements Flags {

    private static OptionParser<WriteOptions> parser = new OptionParser<>();

    static {
        // TODO: These are all placeholders and not yet parsed
        parser.enumFlag(internAtom("back_quotes"), BackQuotes.class, (o, v) -> o.backQuotes = v);
        parser.booleanFlag(internAtom("brace_terms"), (o, v) -> o.braceTerms = v);
        parser.booleanFlag(internAtom("character_escapes"), (o, v) -> o.characterEscapes = v);
        parser.booleanFlag(internAtom("cycles"), (o, v) -> o.cycles = v);
        parser.booleanFlag(internAtom("dotlists"), (o, v) -> o.dotlists = v);
        parser.booleanFlag(internAtom("fullstop"), (o, v) -> o.fullstop = v);
        parser.booleanFlag(internAtom("ignore_ops"), (o, v) -> o.ignoreOps = v);
        parser.intFlag(internAtom("max_depth"), (o, v) -> o.maxDepth = v);
        parser.booleanFlag(internAtom("nl"), (o, v) -> o.nl = v);
        parser.booleanFlag(internAtom("no_lists"), (o, v) -> o.noLists = v);
        parser.booleanFlag(internAtom("numbervars"), (o, v) -> o.numbervars = v);
        parser.booleanFlag(internAtom("partial"), (o, v) -> o.partial = v);
        parser.intFlag(internAtom("priority"), (o, v) -> o.priority = v);
        parser.booleanFlag(internAtom("quoted"), (o, v) -> o.quoted = v);
        parser.enumFlag(internAtom("spacing"), Spacing.class, (o, v) -> o.spacing = v);
        parser.other(internAtom("variable_names"), (o, v) -> o.variableNames = Optional.of(v));
    }

    public BackQuotes backQuotes = BackQuotes.ATOM_string;
    public boolean braceTerms = true;
    public boolean characterEscapes = true; // SWI Prolog sets this depending on global flag
    public boolean cycles = false;
    public boolean dotlists = false;
    public boolean fullstop = false;
    public boolean ignoreOps = false;
    public int maxDepth = 0;
    public boolean nl = false;
    public boolean noLists = false;
    public boolean numbervars = false;
    public boolean partial = false;
    public int priority = 1200;
    public boolean quoted = false;
    public Spacing spacing = Spacing.ATOM_standard;
    public Optional<Term> variableNames = Optional.empty();

    /**
     * Set this object of options from a list of option terms.
     *
     * @param environment Execution environment
     * @param optionsTerm List of options
     */
    public WriteOptions(Environment environment, Term optionsTerm) {
        try {
            parser.apply(environment, this, optionsTerm);
        } catch (FutureFlagError ffe) {
            throw PrologDomainError.error(environment, environment.getAtom("write_option"), ffe.getTerm(), ffe);
        }
    }

    public enum BackQuotes {
        ATOM_string,
        ATOM_symbol_char,
        ATOM_codes
    }

    public enum Spacing {
        ATOM_standard,
        ATOM_next_argument
    }
}
