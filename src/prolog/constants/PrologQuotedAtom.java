// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.constants;

/**
 * A string that behaves as an atom, that was parsed as being quoted
 */
public class PrologQuotedAtom extends PrologAtom {
    public PrologQuotedAtom(String name) {
        super(name);
    }
}
