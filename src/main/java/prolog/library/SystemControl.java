// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Predicate;
import prolog.cli.Run;
import prolog.constants.PrologInteger;
import prolog.exceptions.PrologAborted;
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

    /**
     * Abort current execution
     * @param environment Execution environment
     */
    @Predicate("abort")
    public static void abort(Environment environment) {
        throw PrologAborted.abort(environment);
    }

    /**
     * Break current execution
     * @param environment Execution environment
     */
    @Predicate("break")
    public static void doBreak(Environment environment) {
        Environment child = new Environment(environment);
        new Run(child).run();
    }
}
