// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.bootstrap;

import org.jprolog.constants.PrologAtomInterned;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

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

    // must be first! Note, while this is a weak hash map (per caching intern contract) each added atom will actually
    // maintain references to interned atoms
    private static final Map<PrologAtomInterned.Holder, WeakReference<PrologAtomInterned.Holder>> internedAtoms = new HashMap<>();

    // list of interned atoms
    public static final PrologAtomInterned NULL_ATOM = internAtom("");
    public static final PrologAtomInterned CLAUSE_FUNCTOR = internAtom(":-");
    public static final PrologAtomInterned QUERY_FUNCTOR = internAtom("?-");
    public static final PrologAtomInterned TRUE_ATOM = internAtom("true");
    public static final PrologAtomInterned FALSE_ATOM = internAtom("false");
    public static final PrologAtomInterned ON_ATOM = internAtom("on");
    public static final PrologAtomInterned OFF_ATOM = internAtom("off");
    public static final PrologAtomInterned ABORT_ATOM = internAtom("abort");
    public static final PrologAtomInterned LIST_FUNCTOR = internAtom(".");
    public static final PrologAtomInterned DOT = LIST_FUNCTOR; // alias
    public static final PrologAtomInterned COMMA_FUNCTOR = internAtom(",");
    public static final PrologAtomInterned SEMICOLON_FUNCTOR = internAtom(";");
    public static final PrologAtomInterned BAR_FUNCTOR = internAtom("|");
    public static final PrologAtomInterned CAROT_FUNCTOR = internAtom("^");
    public static final PrologAtomInterned EQUALS_FUNCTOR = internAtom("=");
    public static final PrologAtomInterned EQUALS_ATOM = EQUALS_FUNCTOR;
    public static final PrologAtomInterned LESS_THAN_ATOM = internAtom("<");
    public static final PrologAtomInterned GREATER_THAN_ATOM = internAtom(">");
    public static final PrologAtomInterned EMPTY_BRACES_ATOM = internAtom("{}");
    public static final PrologAtomInterned EMPTY_LIST_ATOM = internAtom("[]");
    public static final PrologAtomInterned OPEN_BRACKET = internAtom("(");
    public static final PrologAtomInterned CLOSE_BRACKET = internAtom(")");
    public static final PrologAtomInterned OPEN_SQUARE_BRACKET = internAtom("[");
    public static final PrologAtomInterned CLOSE_SQUARE_BRACKET = internAtom("]");
    public static final PrologAtomInterned OPEN_BRACES = internAtom("{");
    public static final PrologAtomInterned CLOSE_BRACES = internAtom("}");
    public static final PrologAtomInterned MINUS_ATOM = internAtom("-");
    public static final PrologAtomInterned PLUS_ATOM = internAtom("+");
    public static final PrologAtomInterned SLASH_ATOM = internAtom("/");
    public static final PrologAtomInterned IF_FUNCTOR = internAtom("->");
    public static final PrologAtomInterned LIBRARY_FUNCTOR = internAtom("library");
    public static final PrologAtomInterned UNKNOWN_ATOM = internAtom("unknown");
    public static final PrologAtomInterned CALL_FUNCTOR = internAtom("call");
    public static final PrologAtomInterned ERROR_FUNCTOR = internAtom("error");
    public static final PrologAtomInterned CONTEXT_FUNCTOR = internAtom("context");
    public static final PrologAtomInterned SYSTEM_ERROR_FUNCTOR = internAtom("system_error");
    public static final PrologAtomInterned TYPE_ERROR_FUNCTOR = internAtom("type_error");
    public static final PrologAtomInterned DOMAIN_ERROR_FUNCTOR = internAtom("domain_error");
    public static final PrologAtomInterned SYNTAX_ERROR_FUNCTOR = internAtom("syntax_error");
    public static final PrologAtomInterned INSTANTIATION_ERROR_ATOM = internAtom("instantiation_error");
    public static final PrologAtomInterned EXISTENCE_ERROR_FUNCTOR = internAtom("existence_error");
    public static final PrologAtomInterned PERMISSION_ERROR_FUNCTOR = internAtom("permission_error");
    public static final PrologAtomInterned REPRESENTATION_ERROR_FUNCTOR = internAtom("representation_error");
    public static final PrologAtomInterned EVALUATION_ERROR_FUNCTOR = internAtom("evaluation_error");
    public static final PrologAtomInterned ABORTED_ATOM = internAtom("$aborted");
    public static final PrologAtomInterned LIST_TYPE = internAtom("list");
    public static final PrologAtomInterned CHARACTER_TYPE = internAtom("character");
    public static final PrologAtomInterned IN_CHARACTER_TYPE = internAtom("in_character");
    public static final PrologAtomInterned NUMBER_TYPE = internAtom("number");
    public static final PrologAtomInterned INTEGER_TYPE = internAtom("integer");
    public static final PrologAtomInterned COMPOUND_TYPE = internAtom("compound");
    public static final PrologAtomInterned ATOM_TYPE = internAtom("atom");
    public static final PrologAtomInterned ATOMIC_TYPE = internAtom("atomic");
    public static final PrologAtomInterned CALLABLE_TYPE = internAtom("callable");
    public static final PrologAtomInterned EVALUABLE_TYPE = internAtom("evaluable");
    public static final PrologAtomInterned PREDICATE_INDICATOR_TYPE = internAtom("predicate_indicator");
    public static final PrologAtomInterned ACCESS_ACTION = internAtom("access");
    public static final PrologAtomInterned MODIFY_ACTION = internAtom("modify");
    public static final PrologAtomInterned COMPOUND_OR_ATOM_TYPE = internAtom("compound_or_atom");
    public static final PrologAtomInterned STREAM_TYPE = internAtom("stream");
    public static final PrologAtomInterned PRIVATE_PROCEDURE_TYPE = internAtom("private_procedure");
    public static final PrologAtomInterned STATIC_PROCEDURE_TYPE = internAtom("static_procedure");
    public static final PrologAtomInterned NOT_LESS_THAN_ZERO_DOMAIN = internAtom("not_less_than_zero");
    public static final PrologAtomInterned NON_EMPTY_LIST_DOMAIN = internAtom("non_empty_list");
    public static final PrologAtomInterned OUT_OF_RANGE_DOMAIN = internAtom("out_of_range");
    public static final PrologAtomInterned STREAM_OR_ALIAS_DOMAIN = internAtom("stream_or_alias");
    public static final PrologAtomInterned STREAM_DOMAIN = internAtom("stream");
    public static final PrologAtomInterned STREAM_PROPERTY_DOMAIN = internAtom("stream_property");
    public static final PrologAtomInterned PROCEDURE = internAtom("procedure");
    public static final PrologAtomInterned SOURCE_SINK_DOMAIN = internAtom("source_sink");
    public static final PrologAtomInterned IO_MODE_DOMAIN = internAtom("io_mode");
    public static final PrologAtomInterned OPERATOR_PRIORITY_DOMAIN = internAtom("operator_priority");
    public static final PrologAtomInterned OPERATOR_SPECIFIER_DOMAIN = internAtom("operator_specifier");
    public static final PrologAtomInterned CHARACTER_CODE_LIST_DOMAIN = internAtom("character_code_list");
    public static final PrologAtomInterned CHARACTER_CODE_REPRESENTATION = internAtom("character_code");
    public static final PrologAtomInterned MAX_ARITY_REPRESENTATION = internAtom("max_arity");
    public static final PrologAtomInterned ZERO_DIVISOR_EVALUATION = internAtom("zero_divisor");
    public static final PrologAtomInterned DOLLAR_VAR = internAtom("$VAR");
    public static final PrologAtomInterned OP_FX = internAtom("fx");
    public static final PrologAtomInterned OP_FY = internAtom("fy");
    public static final PrologAtomInterned OP_XF = internAtom("xf");
    public static final PrologAtomInterned OP_YF = internAtom("yf");
    public static final PrologAtomInterned OP_XFX = internAtom("xfx");
    public static final PrologAtomInterned OP_XFY = internAtom("xfy");
    public static final PrologAtomInterned OP_YFX = internAtom("yfx");

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
    public static PrologAtomInterned internAtom(String name) {
        if (used) {
            // Catch late interning, e.g. class was not referenced until after first Environment built,
            // or after interned atoms copied.
            throw new InternalError("Called after interned table used");
        }
        return PrologAtomInterned.get(name, internedAtoms);
    }

    /**
     * Retrieve all interned atoms for copying to an Environment.
     *
     * @return Map of atoms.
     */
    public static Map<PrologAtomInterned.Holder, WeakReference<PrologAtomInterned.Holder>> getInterned() {
        used = true;
        return Collections.unmodifiableMap(internedAtoms);
    }
}
