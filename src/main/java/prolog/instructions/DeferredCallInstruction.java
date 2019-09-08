// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.exceptions.PrologInstantiationError;
import prolog.execution.CallifyTerm;
import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.expressions.Term;
import prolog.predicates.ClauseEntry;

import java.util.Optional;

/**
 * Compile instruction on the fly at the time of the call.
 */
public class DeferredCallInstruction implements Instruction {
    private final Term callable;

    public DeferredCallInstruction(Term callable) {
        this.callable = callable;
    }

    @Override
    public void invoke(Environment environment) {
        Term bound = callable.resolve(environment.getLocalContext());
        if (!bound.isInstantiated()) {
            throw PrologInstantiationError.error(environment, bound);
        }
        bound = bound.enumTerm(new CallifyTerm(environment, Optional.of(bound)));
        CompileContext context = environment.newCompileContext();
        bound.compile(context);
        context.toInstruction().invoke(environment);
    }
}
