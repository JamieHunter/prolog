// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.flags;

import org.jprolog.bootstrap.Interned;
import org.jprolog.exceptions.FutureFlagError;
import org.jprolog.exceptions.PrologDomainError;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.Term;

import java.util.Optional;

/**
 * Structured options parsed from a list of option atoms, used for reading.
 */
public class ReadOptions implements Flags {

    private static OptionParser<ReadOptions> parser = new OptionParser<>();

    static {
        // TODO: These are all placeholders and not yet parsed
        parser.booleanFlag(Interned.internAtom("backquoted_string"), (o, v) -> o.backquotedString = v);
        parser.booleanFlag(Interned.internAtom("character_escapes"), (o, v) -> o.characterEscapes = v);
        parser.booleanFlag(Interned.internAtom("cycles"), (o, v) -> o.cycles = v);
        parser.booleanFlag(Interned.internAtom("dotlists"), (o, v) -> o.dotlists = v);
        parser.enumFlag(Interned.internAtom("double_quotes"), PrologFlags.Quotes.class, (o, v) -> o.doubleQuotes = v);
        parser.enumFlag(Interned.internAtom("singletons"), Singletons.class, (o, v) -> o.singletons = v);
        parser.enumFlag(Interned.internAtom("syntax_errors"), SyntaxErrors.class, (o, v) -> o.syntaxErrors = v);
        parser.enumFlag(Interned.internAtom("full_stop"), FullStop.class, (o, v) -> o.fullStop = v);
        parser.booleanFlag(Interned.internAtom("var_prefix"), (o, v) -> o.varPrefix = v);
        parser.other(Interned.internAtom("variables"), (o, v) -> o.variables = Optional.of(v));
        parser.other(Interned.internAtom("variable_names"), (o, v) -> o.variableNames = Optional.of(v));
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
     * Specify how to handle missing '.' at end of file.
     */
    public FullStop fullStop = FullStop.ATOM_required;
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
            throw PrologDomainError.error(environment, environment.internAtom("read_option"), ffe.getTerm(), ffe);
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

    public enum FullStop {
        ATOM_required,
        ATOM_optional
    }
}
