// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Predicate;
import org.jprolog.constants.PrologInteger;
import org.jprolog.exceptions.PrologAborted;
import org.jprolog.exceptions.PrologHalt;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.Term;
import org.jprolog.cli.Run;

/**
 * System operations
 */
public final class SystemControl {
    private SystemControl() {
        // Static methods/fields only
    }

    /**
     * Exit prolog - thrown as an exception to allow behavior override.
     *
     * @param environment Execution environment
     */
    @Predicate("halt")
    public static void halt(Environment environment) {
        throw new PrologHalt(0, "halt");
    }

    /**
     * Exit prolog - thrown as an exception to allow behavior override.
     *
     * @param environment Execution environment
     * @param exitCode code to exit with
     */
    @Predicate("halt")
    public static void halt(Environment environment, Term exitCode) {
        throw new PrologHalt(PrologInteger.from(exitCode).toInteger(), "halt");
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
        try {
            new Run(child).run();
        } finally {
            child.release();
        }
    }
}
