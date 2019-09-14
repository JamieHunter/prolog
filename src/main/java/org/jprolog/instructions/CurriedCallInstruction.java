// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.exceptions.PrologInstantiationError;
import org.jprolog.exceptions.PrologTypeError;
import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.Term;
import org.jprolog.expressions.TermList;

import java.util.ArrayList;
import java.util.List;

/**
 * Performs a curry-apply.
 */
public class CurriedCallInstruction extends AbstractComposedCallInstruction {
    private final Term args;

    public CurriedCallInstruction(Term callable, Term args) {
        super(callable);
        this.args = args;
    }

    @Override
    protected void addMembers(Environment environment, ArrayList<Term> members) {
        LocalContext context = environment.getLocalContext();
        Term boundArgs = args.resolve(context);
        if (!boundArgs.isInstantiated()) {
            throw PrologInstantiationError.error(environment, boundArgs);
        }
        if (TermList.isList(boundArgs)) {
            List<Term> list = TermList.extractList(boundArgs);
            members.addAll(list);
        } else {
            throw PrologTypeError.listExpected(environment, boundArgs);
        }
    }
}
