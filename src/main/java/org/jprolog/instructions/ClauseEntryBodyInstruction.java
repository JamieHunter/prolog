// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.execution.CompileContext;
import org.jprolog.execution.Instruction;
import org.jprolog.expressions.Term;
import org.jprolog.predicates.ClauseEntry;

/**
 * Defers compilation of clause entry body.
 */
public class ClauseEntryBodyInstruction extends AbstractDeferredCompileInstruction {
    private final Term body;
    private final ClauseEntry clauseEntry;

    public ClauseEntryBodyInstruction(Term body, ClauseEntry clauseEntry) {
        this.body = body;
        this.clauseEntry = clauseEntry;
    }

    @Override
    public Term begin() {
        return clauseEntry.getBody();
    }

    @Override
    public Instruction complete(CompileContext context) {
        return context.toInstruction(clauseEntry);
    }
}
