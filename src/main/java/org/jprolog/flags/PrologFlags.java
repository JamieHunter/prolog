// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.flags;

import org.jprolog.bootstrap.Interned;
import org.jprolog.constants.Atomic;
import org.jprolog.constants.PrologAtom;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.exceptions.FutureRepresentationError;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.Term;

import java.util.Map;
import java.util.TreeMap;

/**
 * Global flags and options
 */
public class PrologFlags implements Flags {

    private static final PrologFlagsParser global = new PrologFlagsParser();
    private static final long DEFAULT_MAX_ARITY = 255;

    public class Scope {
        public final Environment environment;
        public final PrologFlags flags;

        public Scope(Environment environment, PrologFlags flags) {
            this.environment = environment;
            this.flags = flags;
        }
    }

    static {
        global.enumFlag(Interned.internAtom("back_quotes"), Quotes.class, (o, v) -> o.flags.backQuotes = v).
                readEnum(Quotes.class, o -> o.flags.backQuotes).protect();
        global.enumFlag(Interned.internAtom("double_quotes"), Quotes.class, (o, v) -> o.flags.doubleQuotes = v).
                readEnum(Quotes.class, o -> o.flags.doubleQuotes).protect();
        global.protectedFlag(Interned.internAtom("bounded")).
                constant(Interned.FALSE_ATOM).protect();
        global.protectedFlag(Interned.internAtom("break_level")).
                readInteger(o -> (long) o.flags.breakLevel).protect();
        global.booleanFlag(Interned.internAtom("character_escapes"), (o, v) -> o.flags.characterEscapes = v).
                readBoolean(o -> o.flags.characterEscapes).protect();
        global.onOffFlag(Interned.internAtom("char_conversion"), (o, v) -> o.flags.charConversion = v).
                readOnOff(o -> o.flags.charConversion).protect();
        global.onOffFlag(Interned.internAtom("debug"), (o, v) -> {
            o.environment.enableDebugger(v);
        }).
                readOnOff(o -> o.environment.isDebuggerEnabled()).protect();
        global.enumFlag(Interned.internAtom("encoding"), StreamProperties.Encoding.class, (o, v) -> o.flags.encoding = v).
                readEnum(StreamProperties.Encoding.class, o -> o.flags.encoding).protect();
        global.atomFlag(Interned.internAtom("float_format"), (o, v) -> o.flags.floatFormat = v).
                read(o -> o.flags.floatFormat).protect();
        global.protectedFlag(Interned.internAtom("integer_rounding_function")).
                constant(Interned.internAtom("toward_zero")).protect();
        global.intFlag(Interned.internAtom("max_arity"), (o, v) -> o.flags.maxArity = validateMaxArity(v)).
                readInteger(o -> o.flags.maxArity).protect();
        global.enumFlag(Interned.internAtom("unknown"), Unknown.class, (o, v) -> o.flags.unknown = v).
                readEnum(Unknown.class, o -> o.flags.unknown).protect();
    }

    //private final Environment environment;
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
     * Handling of character conversion
     */
    public boolean charConversion = false;
    /**
     * Default file encoding
     */
    public StreamProperties.Encoding encoding = StreamProperties.Encoding.ATOM_utf8;
    /**
     * Default float format
     */
    public PrologAtomLike floatFormat = new PrologAtom("%g");
    /**
     * Maximum arity - this is a soft maximum
     */
    public long maxArity = 255;
    /**
     * What to do if a predicate is not found
     */
    public Unknown unknown = Unknown.ATOM_error;

    // Flags created via create_prolog_flag
    private final Map<Atomic, Term> otherFlags = new TreeMap<>();

    /**
     * Create a new PrologFlags
     */
    public PrologFlags() {
        this.local = new PrologFlagsParser(global);
    }

    /**
     * Get flag
     *
     * @param environment Execution environment
     * @param key         Flag key
     * @return Flag value as a term
     */
    public Term get(Environment environment, Atomic key) {
        Scope scope = new Scope(environment, this);
        return local.get(scope, key);
    }

    /**
     * Update flag
     *
     * @param environment Execution environment
     * @param key         Flag key
     * @param value       New value as a term
     */
    public void set(Environment environment, Atomic key, Term value) {
        Scope scope = new Scope(environment, this);
        local.set(scope, key, value);
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

    /**
     * @param environment Execution environment
     * @return merged set of flags
     */
    public Map<Atomic, Term> getAll(Environment environment) {
        Scope scope = new Scope(environment, this);
        return local.getAll(scope);
    }

    /**
     * While this version of prolog permits increasing MAX_ARITY, MAX_ARITY cannot be reduced.
     * @param v New value
     * @return new value
     */
    private static long validateMaxArity(long v) {
        if (v < DEFAULT_MAX_ARITY || v > Integer.MAX_VALUE) {
            throw new FutureRepresentationError(Interned.MAX_ARITY_REPRESENTATION);
        }
        return v;
    }

    public enum Quotes {
        ATOM_atom,
        ATOM_codes,
        ATOM_symbol_char,
        ATOM_chars,
        ATOM_string
    }

    public enum Unknown {
        ATOM_fail,
        ATOM_warning,
        ATOM_error
    }

}
