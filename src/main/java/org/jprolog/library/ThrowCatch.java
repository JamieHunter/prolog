// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Predicate;
import org.jprolog.execution.CompileContext;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.Term;
import org.jprolog.instructions.ExecBlock;
import org.jprolog.instructions.ExecCatch;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Bootstraps throw/catch predicates.
 */
public final class ThrowCatch {
    private ThrowCatch() {
        // Static methods/fields only
    }

    /**
     * Calls callTerm. If during execution of callTerm an exception occurs,
     * and that exception matches matchTerm, then the exception is recovered
     * via recoverTerm prior to continued execution.
     *
     * @param compiling Compiling context
     * @param source Catch clause
     */
    @Predicate(value = "catch", arity = 3)
    public static void prologCatch(CompileContext compiling, CompoundTerm source) {
        Term callTerm = source.get(0);
        Term matchTerm = source.get(1);
        Term recoverTerm = source.get(2);
        compiling.add(source, new ExecCatch(
                ExecBlock.deferred(callTerm),
                matchTerm,
                ExecBlock.deferred(recoverTerm)));
    }

    /**
     * Execute a prolog throw. Note that this does not throw a Java exception, rather it is
     * a special case of backtracking. Some Java exceptions on the other hand are converted to
     * prolog Throw's
     * @param environment Execution environment
     * @param throwTerm Term representing throw reason
     */
    @Predicate("throw")
    public static void prologThrow(Environment environment, Term throwTerm) {
        // switch from forward to throwing
        environment.throwing(throwTerm);
    }
}
