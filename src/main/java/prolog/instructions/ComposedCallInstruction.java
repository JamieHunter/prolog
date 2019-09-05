// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.expressions.Term;

import java.util.ArrayList;

/**
 * Performs a curry-apply, where the args is a bounded list.
 */
public class ComposedCallInstruction extends AbstractComposedCallInstruction {
    private final Term[] args;

    public ComposedCallInstruction(Term callable, Term[] args) {
        super(callable);
        this.args = args;
    }

    @Override
    protected void addMembers(Environment environment, ArrayList<Term> members) {
        LocalContext context = environment.getLocalContext();
        for (int i = 0; i < args.length; i++) {
            members.add(args[i].resolve(context));
        }
    }
}
