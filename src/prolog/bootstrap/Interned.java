// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.bootstrap;

import prolog.constants.PrologAtom;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * These are interned atoms, that is, they are valid always across all environments. Other interned atoms exist within
 * library classes (must be referenced).
 * <p>
 * TODO: Many of these need to be refactored out.
 */
public final class Interned {

    private Interned() {
        // Utility
    }

    // must be first!
    private static final HashMap<String, PrologAtom> internedAtoms = new HashMap<>();

    // list of interned atoms
    public static final PrologAtom CLAUSE_FUNCTOR = internAtom(":-");
    public static final PrologAtom QUERY_FUNCTOR = internAtom("?-");
    public static final PrologAtom TRUE_ATOM = internAtom("true");
    public static final PrologAtom FALSE_ATOM = internAtom("false");
    public static final PrologAtom LIST_FUNCTOR = internAtom(".");
    public static final PrologAtom DOT = LIST_FUNCTOR; // alias
    public static final PrologAtom COMMA_FUNCTOR = internAtom(",");
    public static final PrologAtom SEMICOLON_FUNCTOR = internAtom(";");
    public static final PrologAtom BAR_FUNCTOR = internAtom("|");
    public static final PrologAtom EQUALS_FUNCTOR = internAtom("=");
    public static final PrologAtom EMPTY_BRACES_ATOM = internAtom("{}");
    public static final PrologAtom EMPTY_LIST_ATOM = internAtom("[]");
    public static final PrologAtom OPEN_BRACKET = internAtom("(");
    public static final PrologAtom CLOSE_BRACKET = internAtom(")");
    public static final PrologAtom OPEN_SQUARE_BRACKET = internAtom("[");
    public static final PrologAtom CLOSE_SQUARE_BRACKET = internAtom("]");
    public static final PrologAtom OPEN_BRACES = internAtom("{");
    public static final PrologAtom CLOSE_BRACES = internAtom("}");
    public static final PrologAtom MINUS_ATOM = internAtom("-");
    public static final PrologAtom PLUS_ATOM = internAtom("+");
    public static final PrologAtom SLASH_ATOM = internAtom("/");
    public static final PrologAtom IF_FUNCTOR = internAtom("->");
    public static final PrologAtom LIBRARY_FUNCTOR = internAtom("library");
    public static final PrologAtom UNKNOWN_ATOM = internAtom("unknown");
    public static final PrologAtom CALL_FUNCTOR = internAtom("call");
    public static final PrologAtom ERROR_FUNCTOR = internAtom("error");
    public static final PrologAtom CONTEXT_FUNCTOR = internAtom("context");
    public static final PrologAtom SYSTEM_ERROR_FUNCTOR = internAtom("system_error");
    public static final PrologAtom TYPE_ERROR_FUNCTOR = internAtom("type_error");
    public static final PrologAtom DOMAIN_ERROR_FUNCTOR = internAtom("domain_error");
    public static final PrologAtom SYNTAX_ERROR_FUNCTOR = internAtom("syntax_error");
    public static final PrologAtom INSTANTIATION_ERROR_ATOM = internAtom("instantiation_error");
    public static final PrologAtom EXISTENCE_ERROR_FUNCTOR = internAtom("existence_error");
    public static final PrologAtom PERMISSION_ERROR_FUNCTOR = internAtom("permission_error");
    public static final PrologAtom REPRESENTATION_ERROR_FUNCTOR = internAtom("representation_error");
    public static final PrologAtom EVALUATION_ERROR_FUNCTOR = internAtom("evaluation_error");
    public static final PrologAtom LIST_TYPE = internAtom("list");
    public static final PrologAtom CHARACTER_TYPE = internAtom("character");
    public static final PrologAtom IN_CHARACTER_TYPE = internAtom("in_character");
    public static final PrologAtom NUMBER_TYPE = internAtom("number");
    public static final PrologAtom INTEGER_TYPE = internAtom("integer");
    public static final PrologAtom COMPOUND_TYPE = internAtom("compound");
    public static final PrologAtom ATOM_TYPE = internAtom("atom");
    public static final PrologAtom CALLABLE_TYPE = internAtom("callable");
    public static final PrologAtom EVALUABLE_TYPE = internAtom("evaluable");
    public static final PrologAtom COMPOUND_OR_ATOM_TYPE = internAtom("compound_or_atom");
    public static final PrologAtom STREAM_TYPE = internAtom("stream");
    public static final PrologAtom NOT_LESS_THAN_ZERO_DOMAIN = internAtom("not_less_than_zero");
    public static final PrologAtom OUT_OF_RANGE_DOMAIN = internAtom("out_of_range");
    public static final PrologAtom STREAM_OR_ALIAS_DOMAIN = internAtom("stream_or_alias");
    public static final PrologAtom STREAM_PROPERTY_DOMAIN = internAtom("stream_property");
    public static final PrologAtom PROCEDURE = internAtom("procedure");
    public static final PrologAtom SOURCE_SINK_DOMAIN = internAtom("source_sink");
    public static final PrologAtom IO_MODE_DOMAIN = internAtom("io_mode");
    public static final PrologAtom OPERATOR_PRIORITY_DOMAIN = internAtom("operator_priority");
    public static final PrologAtom CHARACTER_CODE_LIST_DOMAIN = internAtom("character_code_list");
    public static final PrologAtom CHARACTER_CODE_REPRESENTATION = internAtom("character_code");
    public static final PrologAtom ZERO_DIVISOR_EVALUATION = internAtom("zero_divisor");
    public static final PrologAtom OP_FX = internAtom("fx");
    public static final PrologAtom OP_FY = internAtom("fy");
    public static final PrologAtom OP_XF = internAtom("xf");
    public static final PrologAtom OP_YF = internAtom("yf");
    public static final PrologAtom OP_XFX = internAtom("xfx");
    public static final PrologAtom OP_XFY = internAtom("xfy");
    public static final PrologAtom OP_YFX = internAtom("yfx");

    //
    // Debugging aid. This is to catch errors when atoms are interned after they are used.
    //
    private static boolean used = false;

    /**
     * Create and/or retrieve an atom that is made available to all environments. Only one atom exists per name. This
     * method must be called before any Environments have been created - more strictly, during the bootstrap process
     * before interned atoms are copied to the first environment.
     *
     * @param name Name of atom
     * @return Atom
     */
    public static PrologAtom internAtom(String name) {
        if (used) {
            // Catch late interning, e.g. class was not referenced until after first Environment built,
            // or after interned atoms copied.
            throw new InternalError("Called after interned table used");
        }
        return internedAtoms.computeIfAbsent(name, PrologAtom::internalNew);
    }

    /**
     * Retrieve all interned atoms for copying to an Environment.
     *
     * @return Map of atoms.
     */
    public static Map<String, PrologAtom> getInterned() {
        used = true;
        return Collections.unmodifiableMap(internedAtoms);
    }
}
