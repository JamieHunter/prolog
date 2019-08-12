// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Predicate;
import prolog.execution.Environment;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Debugging utilities
 */
public final class Debug {
    private Debug() {
        // Static methods/fields only
    }

    /**
     * Non-standard Prolog, to allow setting of Java breakpoing
     *
     * @param environment Execution environment
     */
    @Predicate("@@@@")
    public static void debugBreak(Environment environment) {
        // set breakpoint here in debugger
    }
}
