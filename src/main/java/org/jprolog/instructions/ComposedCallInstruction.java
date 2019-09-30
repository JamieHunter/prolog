// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.Term;

import java.util.ArrayList;

/**
 * Performs a curry-apply.
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
        for (Term arg : args) {
            members.add(arg.resolve(context));
        }
    }
}
