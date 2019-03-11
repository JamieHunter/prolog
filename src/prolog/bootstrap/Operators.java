// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.bootstrap;

import prolog.constants.Atomic;
import prolog.execution.OperatorEntry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * All pre-defined operators with precedence.
 */
public class Operators {

    // tables of default operators
    // Must be before the construction list below.
    private static final HashMap<Atomic, OperatorEntry> infixPostfixOperatorTable = new HashMap<>();
    private static final HashMap<Atomic, OperatorEntry> prefixOperatorTable = new HashMap<>();
    public static final OperatorEntry COMMA;

    // List of default operators and precedence. Note that the precedence numbers are standardized
    static {
        //
        // This table includes a number of operators that are not yet implemented
        //
        makeOperator(1200, OperatorEntry.Code.XFX, "-->", ":-");
        makeOperator(1200, OperatorEntry.Code.FX, "?-", ":-");
        makeOperator(1100, OperatorEntry.Code.XFY, ";");
        makeOperator(1100, OperatorEntry.Code.XFY, "|");
        makeOperator(1050, OperatorEntry.Code.XFY, "->", "*->");
        COMMA = makeOperator(OperatorEntry.COMMA, OperatorEntry.Code.XFY, ",");
        makeOperator(990, OperatorEntry.Code.XFX, ":=");
        makeOperator(900, OperatorEntry.Code.FY, "\\+", "not", "spy");
        makeOperator(700, OperatorEntry.Code.XFX,
                "<", "=", "=..", "=@=", "\\=@=", "=:=", "=<", "==", "=\\=",
                ">", ">=", "@<", "@=<", "@>", "@>=",
                "\\=", "\\==", "as", "is", ">:<", ":<"
        );
        makeOperator(600, OperatorEntry.Code.XFY, ":");
        makeOperator(500, OperatorEntry.Code.YFX, "+", "-", "/\\", "\\/", "xor");
        makeOperator(500, OperatorEntry.Code.FX, "?");
        makeOperator(400, OperatorEntry.Code.YFX, "*", "/", "//", "mod");
        makeOperator(200, OperatorEntry.Code.XFX, "**");
        makeOperator(200, OperatorEntry.Code.XFY, "^");
        makeOperator(200, OperatorEntry.Code.FY, "+", "-", "\\");
        makeOperator(100, OperatorEntry.Code.YFX, ".");
        makeOperator(1, OperatorEntry.Code.FX, "$");
    }

    private Operators() {
    }

    //
    // ===================================================================
    //

    /**
     * Create operator entry and add to correct table.
     *
     * @param functor Functor of operator
     * @param prefix  True if this is a prefix operator
     * @return New operator entry
     */
    private static OperatorEntry makeEntry(Atomic functor, boolean prefix) {
        if (prefix) {
            return prefixOperatorTable.computeIfAbsent(functor, OperatorEntry::new);
        } else {
            return infixPostfixOperatorTable.computeIfAbsent(functor, OperatorEntry::new);
        }
    }

    /**
     * Create an operator entry into the operator table.
     *
     * @param precedence Precedence to add at
     * @param code       operator code
     * @param names      List of functor names to add
     * @return last operator added
     */
    private static OperatorEntry makeOperator(int precedence, OperatorEntry.Code code, String... names) {
        OperatorEntry entry = null;
        for (String name : names) {
            entry = makeEntry(Interned.internAtom(name), code.isPrefix());
            entry.setCode(code);
            entry.setPrecedence(precedence);
        }
        return entry;
    }

    /**
     * Retrieve default infix/postfix operators
     *
     * @return operator table
     */
    public static Map<? extends Atomic, ? extends OperatorEntry> getInfixPostfix() {
        return Collections.unmodifiableMap(infixPostfixOperatorTable);
    }

    /**
     * Retrieve default prefix operators
     *
     * @return operator table
     */
    public static Map<? extends Atomic, ? extends OperatorEntry> getPrefix() {
        return Collections.unmodifiableMap(prefixOperatorTable);
    }
}
