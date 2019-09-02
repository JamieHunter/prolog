// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.exceptions.PrologInstantiationError;
import prolog.exceptions.PrologTypeError;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.expressions.Term;
import prolog.expressions.TermList;

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
