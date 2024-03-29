// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.exceptions.PrologInstantiationError;
import org.jprolog.enumerators.CallifyTerm;
import org.jprolog.execution.CompileContext;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.expressions.Term;

import java.util.Optional;

/**
 * Compile instruction on the fly at the time of the instruction. This should generally be wrapped directly or
 * indirectly in a call variant.
 */
public class ExecFuture implements Instruction {
    private final Term future;

    public ExecFuture(Term future) {
        this.future = future;
    }

    @Override
    public void invoke(Environment environment) {
        Term bound = future.resolve(environment.getLocalContext());
        if (!bound.isInstantiated()) {
            throw PrologInstantiationError.error(environment, bound);
        }
        bound = bound.enumTerm(new CallifyTerm(environment, bound));
        CompileContext context = environment.newCompileContext();
        bound.compile(context);
        context.toInstruction().invoke(environment);
    }
}
