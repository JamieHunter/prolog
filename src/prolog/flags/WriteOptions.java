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
        parser.enumFlag(internAtom("back_quotes"), PrologFlags.Quotes.class, (o, v) -> o.backQuotes = v);
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

    /**
     * Indicate how to handle back-quotes
     */
    public PrologFlags.Quotes backQuotes;
    /**
     * Indicate how to handle character escapes
     */
    public boolean characterEscapes;
    /**
     * Write {} term as {...}
     */
    public boolean braceTerms = true;
    /**
     * Write cycles as a special term @(Template, Substitutions)
     */
    public boolean cycles = false;
    /**
     * Write lists using dotted notation rather than list notation
     */
    public boolean dotlists = false;
    /**
     * Write a '.' at end of term
     */
    public boolean fullstop = false;
    /**
     * Ignore operator precedence
     */
    public boolean ignoreOps = false;
    /**
     * Write ellipses if depth is greater than this index, 0 indicates unlimited.
     */
    public int maxDepth = 0;
    /**
     * Write new line at end of term
     */
    public boolean nl = false;
    /**
     * Alternative to dotlists, allowing for [|]
     */
    public boolean noLists = false;
    /**
     * Special handling of $VAR terms
     */
    public boolean numbervars = false;
    /**
     * if true, do not reset spacing state engine
     */
    public boolean partial = false;
    /**
     * Specify assumed priority for term being written
     */
    public int priority = 1200;
    /**
     * If true, quote atoms that need quoting
     */
    public boolean quoted = false;
    /**
     * Specify spacing behavior
     */
    public Spacing spacing = Spacing.ATOM_standard;
    /**
     * provide names for variables
     */
    public Optional<Term> variableNames = Optional.empty();

    /**
     * Set this object of options from a list of option terms.
     *
     * @param environment Execution environment
     * @param optionsTerm List of options
     */
    public WriteOptions(Environment environment, Term optionsTerm) {
        PrologFlags flags = environment.getFlags();
        try {
            backQuotes = flags.backQuotes;
            characterEscapes = flags.characterEscapes;
            parser.apply(environment, this, optionsTerm);
        } catch (FutureFlagError ffe) {
            throw PrologDomainError.error(environment, environment.getAtom("write_option"), ffe.getTerm(), ffe);
        }
    }

    public enum Spacing {
        ATOM_standard,
        ATOM_next_argument
    }
}
