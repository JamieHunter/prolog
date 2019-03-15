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

import static prolog.bootstrap.Interned.internAtom;

/**
 * Global flags and options
 */
public class PrologFlags implements Flags {

    private static final FlagsParser global = new FlagsParser();
    static {
        global.enumFlag(internAtom("back_quotes"), Quotes.class, (o, v) -> o.backQuotes = v).
                readEnum(Quotes.class, o -> o.backQuotes).protect();
        global.enumFlag(internAtom("double_quotes"), Quotes.class, (o, v) -> o.doubleQuotes = v).
                readEnum(Quotes.class, o -> o.doubleQuotes).protect();
        global.protectedFlag(internAtom("bounded")).
                constant(Interned.FALSE_ATOM).protect();
        global.protectedFlag(internAtom("break_level")).
                readInteger(o -> o.breakLevel).protect();
        global.booleanFlag(internAtom("character_escapes"), (o,v) -> o.characterEscapes = v).
                readBoolean(o -> o.characterEscapes).protect();
        global.booleanFlag(internAtom("debug"), (o,v) -> o.debug = v).
                readBoolean(o -> o.debug).protect();
    }

    private final Environment environment;
    private final FlagsParser local;
    public Quotes backQuotes = Quotes.ATOM_codes;
    public Quotes doubleQuotes = Quotes.ATOM_string;
    public int breakLevel = 0;
    public boolean characterEscapes = true;
    public boolean debug = false;
    // Flags created via create_prolog_flag
    private final Map<Atomic,Term> otherFlags = new HashMap<>();

    public PrologFlags(Environment environment) {
        this.environment = environment;
        this.local = new FlagsParser(global);
    }

    public Environment environment() {
        return environment;
    }

    public Term get(Atomic key) {
        return local.get(this, key);
    }

    public void set(Atomic key, Term value) {
        local.set(this, key, value);
    }

    public void create(Atomic key, Term value, CreateFlagOptions options) {
        local.create(this, key, value, options);
    }

    /*package*/ void setOther(Atomic key, Term value) {
        otherFlags.put(key, value);
    }
    /*package*/ Term getOther(Atomic key) {
        return otherFlags.get(key);
    }

    public enum Quotes {
        ATOM_codes,
        ATOM_symbol_char,
        ATOM_string
    }
}
