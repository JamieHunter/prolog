// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.exceptions;

/**
 * An exception that when thrown, exits the Prolog interpreter completely (cannot be caught).
 */
public class PrologHalt extends RuntimeException {
    private final int haltCode;
    public PrologHalt(int haltCode, String message) {
        super(message);
        this.haltCode = haltCode;
    }

    public int getHaltCode() {
        return haltCode;
    }
}
