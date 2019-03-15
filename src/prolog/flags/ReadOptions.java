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
        parser.enumFlag(internAtom("double_quotes"), DoubleQuotes.class, (o, v) -> o.doubleQuotes = v);
        parser.enumFlag(internAtom("singletons"), Singletons.class, (o, v) -> o.singletons = v);
        parser.enumFlag(internAtom("syntax_errors"), SyntaxErrors.class, (o, v) -> o.syntaxErrors = v);
        parser.booleanFlag(internAtom("var_prefix"), (o, v) -> o.varPrefix = v);
        parser.other(internAtom("variables"), (o, v) -> o.variables = Optional.of(v));
        parser.other(internAtom("variable_names"), (o, v) -> o.variableNames = Optional.of(v));
    }

    public boolean backquotedString = true; // SWI Prolog sets this depending on global flag
    public boolean characterEscapes = true; // SWI Prolog sets this depending on global flag
    public boolean cycles = false;
    public boolean dotlists = false;
    public DoubleQuotes doubleQuotes = DoubleQuotes.ATOM_chars; // Or get from global flag
    public Singletons singletons = Singletons.ATOM_warning;
    public SyntaxErrors syntaxErrors = SyntaxErrors.ATOM_error;
    public boolean varPrefix = false;
    public Optional<Term> variables = Optional.empty();
    public Optional<Term> variableNames = Optional.empty();

    /**
     * Set this object of options from a list of option terms.
     *
     * @param environment Execution environment
     * @param optionsTerm List of options
     */
    public ReadOptions(Environment environment, Term optionsTerm) {
        try {
            parser.apply(environment, this, optionsTerm);
        } catch (FutureFlagError ffe) {
            throw PrologDomainError.error(environment, environment.getAtom("read_option"), ffe.getTerm(), ffe);
        }
    }

    public enum DoubleQuotes {
        ATOM_string,
        ATOM_codes,
        ATOM_chars,
        ATOM_atom
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
