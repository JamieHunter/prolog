// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.flags;

import prolog.bootstrap.Interned;
import prolog.constants.Atomic;
import prolog.execution.Environment;
import prolog.expressions.Term;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static prolog.bootstrap.Interned.internAtom;

/**
 * Global flags and options
 */
public class PrologFlags implements FlagsWithEnvironment {

    private static final PrologFlagsParser global = new PrologFlagsParser();

    static {
        global.enumFlag(internAtom("back_quotes"), Quotes.class, (o, v) -> o.backQuotes = v).
                readEnum(Quotes.class, o -> o.backQuotes).protect();
        global.enumFlag(internAtom("double_quotes"), Quotes.class, (o, v) -> o.doubleQuotes = v).
                readEnum(Quotes.class, o -> o.doubleQuotes).protect();
        global.protectedFlag(internAtom("bounded")).
                constant(Interned.FALSE_ATOM).protect();
        global.protectedFlag(internAtom("break_level")).
                readInteger(o -> o.breakLevel).protect();
        global.booleanFlag(internAtom("character_escapes"), (o, v) -> o.characterEscapes = v).
                readBoolean(o -> o.characterEscapes).protect();
        global.booleanFlag(internAtom("debug"), (o, v) -> o.debug = v).
                readBoolean(o -> o.debug).protect();
        global.enumFlag(internAtom("encoding"), StreamProperties.Encoding.class, (o, v) -> o.encoding = v).
                readEnum(StreamProperties.Encoding.class, o -> o.encoding).protect();
    }

    private final Environment environment;
    private final PrologFlagsParser local;
    /**
     * Handling of back quotes
     */
    public Quotes backQuotes = Quotes.ATOM_codes;
    /**
     * Handling of double quotes
     */
    public Quotes doubleQuotes = Quotes.ATOM_string;
    /**
     * Current break levels
     */
    public int breakLevel = 0;
    /**
     * Handling of character escapes
     */
    public boolean characterEscapes = true;
    /**
     * Mode to make prolog more debuggable
     */
    public boolean debug = false;
    /**
     * Default file encoding
     */
    public StreamProperties.Encoding encoding = StreamProperties.Encoding.ATOM_utf8;

    // Flags created via create_prolog_flag
    private final Map<Atomic, Term> otherFlags = new HashMap<>();

    /**
     * Create a new PrologFlags associated with a new environment
     *
     * @param environment Execution environment
     */
    public PrologFlags(Environment environment) {
        // TODO don't use this if cloning environment
        this.environment = environment;
        this.local = new PrologFlagsParser(global);
    }

    /**
     * @return Execution environment
     */
    @Override
    public Environment environment() {
        return environment;
    }

    /**
     * Get flag
     *
     * @param key Flag key
     * @return Flag value as a term
     */
    public Term get(Atomic key) {
        return local.get(this, key);
    }

    /**
     * Update flag
     *
     * @param key   Flag key
     * @param value New value as a term
     */
    public void set(Atomic key, Term value) {
        local.set(this, key, value);
    }

    /**
     * Create a new flag
     *
     * @param key     Flag key
     * @param value   Initial value
     * @param options Options controlling how this create works
     */
    public void create(Atomic key, Term value, CreateFlagOptions options) {
        local.create(this, key, value, options);
    }

    /**
     * Allows indirect management of values for custom flags
     *
     * @param key   Flag key
     * @param value Flag value
     */
    /*package*/ void setOther(Atomic key, Term value) {
        otherFlags.put(key, value);
    }

    /**
     * Allows indirect management of values for custom flags
     *
     * @param key Flag key
     * @return Flag value
     */
    /*package*/ Term getOther(Atomic key) {
        return otherFlags.get(key);
    }

    public enum Quotes {
        ATOM_codes,
        ATOM_symbol_char,
        ATOM_string
    }
}
