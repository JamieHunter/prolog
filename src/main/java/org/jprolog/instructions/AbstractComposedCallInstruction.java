// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.constants.Atomic;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.exceptions.PrologInstantiationError;
import org.jprolog.exceptions.PrologTypeError;
import org.jprolog.execution.CompileContext;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.CompoundTermImpl;
import org.jprolog.expressions.Term;

import java.util.ArrayList;

/**
 * Performs a curry-apply, where the args is really a compound list
 */
public abstract class AbstractComposedCallInstruction implements Instruction {
    private final Term callable;

    protected AbstractComposedCallInstruction(Term callable) {
        this.callable = callable;
    }

    protected abstract void addMembers(Environment environment, ArrayList<Term> members);

    private Term apply(Environment environment) {
        LocalContext context = environment.getLocalContext();
        Term boundCallable = callable.resolve(context);
        if (!boundCallable.isInstantiated()) {
            throw PrologInstantiationError.error(environment, boundCallable);
        }
        ArrayList<Term> members = new ArrayList<>();
        Atomic functor;
        if (boundCallable instanceof CompoundTerm) {
            CompoundTerm compBound = (CompoundTerm) boundCallable;
            functor = compBound.functor();
            for (int i = 0; i < compBound.arity(); i++) {
                members.add(compBound.get(i));
            }
        } else if (boundCallable.isAtom()) {
            functor = (PrologAtomLike) boundCallable;
        } else {
            throw PrologTypeError.callableExpected(environment, boundCallable);
        }
        addMembers(environment, members);
        return new CompoundTermImpl(functor, members);
    }

    @Override
    public void invoke(Environment environment) {
        Term callable = apply(environment);
        // Compile every time - this may be optimized in future
        CompileContext context = environment.newCompileContext();
        callable.compile(context);
        Instruction inst = context.toInstruction();
        // execute-deferred (aids debugging)
        ExecDefer.defer(environment, inst);
    }
}
