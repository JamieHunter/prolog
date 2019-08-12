// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.expressions;

/**
 * Class containing sorting order of various Term types, used by {@link Term#typeRank()}
 */
public class TypeRank {
    public static final int VARIABLE = 100;
    public static final int FLOAT = 200;
    public static final int INTEGER = 300;
    public static final int ATOM = 400;
    public static final int EMPTY_LIST = 500;
    public static final int STRING = 600;
    public static final int COMPOUND_TERM = 700;
}
