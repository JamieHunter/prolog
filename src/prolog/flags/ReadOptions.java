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
 * Structured options parsed from a list of option atoms, used for reading.
 */
public class ReadOptions implements Flags {

    private static OptionParser<ReadOptions> parser = new OptionParser<>();

    static {
        // TODO: These are all placeholders and not yet parsed
        parser.booleanFlag(internAtom("backquoted_string"), (o, v) -> o.backquotedString = v);
        parser.booleanFlag(internAtom("character_escapes"), (o, v) -> o.characterEscapes = v);
        parser.booleanFlag(internAtom("cycles"), (o, v) -> o.cycles = v);
        parser.booleanFlag(internAtom("dotlists"), (o, v) -> o.dotlists = v);
        parser.enumFlag(internAtom("double_quotes"), PrologFlags.Quotes.class, (o, v) -> o.doubleQuotes = v);
        parser.enumFlag(internAtom("singletons"), Singletons.class, (o, v) -> o.singletons = v);
        parser.enumFlag(internAtom("syntax_errors"), SyntaxErrors.class, (o, v) -> o.syntaxErrors = v);
        parser.booleanFlag(internAtom("var_prefix"), (o, v) -> o.varPrefix = v);
        parser.other(internAtom("variables"), (o, v) -> o.variables = Optional.of(v));
        parser.other(internAtom("variable_names"), (o, v) -> o.variableNames = Optional.of(v));
    }

    /**
     * If true, backquotes produces a string object
     */
    public boolean backquotedString;
    /**
     * If true (default), parse '\' in strings
     */
    public boolean characterEscapes;
    /**
     * Override how double-quotes are handled
     */
    public PrologFlags.Quotes doubleQuotes;
    /**
     * If true, handle the @(Template, Substitution) operator for producing cycling terms
     */
    public boolean cycles = false;
    /**
     * If true, read .(x,y) as a list term.
     */
    public boolean dotlists = false;
    /**
     * Specify how to handle variables that are mentioned only once.
     */
    public Singletons singletons = Singletons.ATOM_warning;
    /**
     * Specify how to handle syntax errors.
     */
    public SyntaxErrors syntaxErrors = SyntaxErrors.ATOM_error;
    /**
     * Force variables to begin with '_'
     */
    public boolean varPrefix = false;
    /**
     * Receives list of variables read
     */
    public Optional<Term> variables = Optional.empty();
    /**
     * Receive list of unifiable variables
     */
    public Optional<Term> variableNames = Optional.empty();

    /**
     * Set this object of options from a list of option terms.
     *
     * @param environment Execution environment
     * @param optionsTerm List of options
     */
    public ReadOptions(Environment environment, Term optionsTerm) {
        PrologFlags flags = environment.getFlags();
        try {
            backquotedString = flags.backQuotes == PrologFlags.Quotes.ATOM_string;
            doubleQuotes = flags.doubleQuotes;
            characterEscapes = flags.characterEscapes;
            parser.apply(environment, this, optionsTerm);
        } catch (FutureFlagError ffe) {
            throw PrologDomainError.error(environment, environment.getAtom("read_option"), ffe.getTerm(), ffe);
        }
    }

    public enum Singletons {
        ATOM_warning
    }

    public enum SyntaxErrors {
        ATOM_error,
        ATOM_fail,
        ATOM_quiet
    }
}
