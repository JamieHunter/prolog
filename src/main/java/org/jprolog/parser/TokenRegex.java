// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.parser;

/**
 * Common regex patterns for parsing and generation. This is pulled in as a base class or utility class.
 */
public class TokenRegex {

    public static final String SOLO_GRAPHIC = "[!(),;\\[\\]{}|$]";
    public static final String GRAPHIC = "[-#$&*+./:<=>?@^~\\\\]++";
    public static final String WS = "[ \\t\\r\\n]";
    public static final String WSs = WS + "++";
    public static final String CAPS = "[A-Z]";
    public static final String SMALL = "[a-z]";
    public static final String ALPHANUMERIC = "[_0-9a-zA-Z]";
    public static final String ALPHA_ATOM = SMALL + ALPHANUMERIC + "*+";
    public static final String VARIABLE = CAPS + ALPHANUMERIC + "*+";
    public static final String UNDERSCORE_VARIABLE = "_" + ALPHANUMERIC + "++";
    public static final String ANONYMOUS = "_[0-9]*+";
    public static final String DECIMAL = "[0-9][0-9_]*+";
    public static final String BINARY = "0b[0-1][0-1_]*+";
    public static final String OCTAL = "0o[0-7][0-7_]*+";
    public static final String FLOAT = DECIMAL + "\\." + DECIMAL + "(?:[eE][-+]?" + DECIMAL + ")?";
    public static final String HEX = "0x[0-9a-fA-F][0-9a-fA-F_]*+";
    public static final String START_CHAR_CODE = "0'";
    public static final String STRING_CHAR = "[^'`\"\\\\]"; // single character, anything not special
    public static final String META_ESCAPE = "\\\\[\\\\\"'`]"; // catches ', ", `, \ escaped
    public static final String CONTROL_ESCAPE = "\\\\[abrftnv]"; // most single character control
    public static final String OCT_ESCAPE = "\\\\[0-7]++\\\\"; // rough detail
    public static final String HEX_ESCAPE = "\\\\x[0-9a-fA-F]*+\\\\"; // rough detail
    public static final String START_STRING = "['`\"]";
    public static final String START_LINE_COMMENT = "%";
    public static final String START_BLOCK_COMMENT = "/\\*";
    public static final String CATCH_ALL = ".?";
    public static final String BACKSLASH_CATCH_ALL = "\\\\.";

    /**
     * Regex helper, construct a regex named group from a pattern.
     *
     * @param name    Name of group, assumed legal regex syntax
     * @param pattern Pattern, assumed legal regex syntax
     * @return Regex grouping construct.
     */
    public static String group(String name, String pattern) {
        return "(?<" + name + ">" + pattern + ")";
    }

    /**
     * Regex helper, combine alternative expressions using the '|' operator.
     *
     * @param first      First regex
     * @param alternates Alternative valid regex's.
     * @return Composed regex expression.
     */
    public static String or(String first, String... alternates) {
        StringBuilder builder = new StringBuilder();
        builder.append("(?:");
        builder.append(first);
        for (String alt : alternates) {
            if (alt == null || alt.isEmpty()) {
                continue;
            }
            builder.append('|');
            builder.append(alt);
        }
        builder.append(')');
        return builder.toString();
    }
}
