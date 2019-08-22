// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Predicate;
import prolog.constants.PrologInteger;
import prolog.execution.Environment;
import prolog.expressions.Term;

/**
 * System operations
 */
public final class SystemControl {
    private SystemControl() {
        // Static methods/fields only
    }

    /**
     * Exit prolog
     *
     * @param environment Execution environment
     */
    @Predicate("halt")
    public static void halt(Environment environment) {
        System.exit(0);
    }

    /**
     * Exit prolog
     *
     * @param environment Execution environment
     * @param exitCode code to exit with
     */
    @Predicate("halt")
    public static void halt(Environment environment, Term exitCode) {
        System.exit(PrologInteger.from(exitCode).toInteger());
    }
}
