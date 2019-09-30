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
    private final ClauseEntry clauseEntry;

    public ClauseEntryBodyInstruction(ClauseEntry clauseEntry) {
        this.clauseEntry = clauseEntry;
    }

    @Override
    protected Term begin() {
        return clauseEntry.getBody();
    }

    @Override
    protected Instruction complete(CompileContext context) {
        return context.toInstruction(clauseEntry);
    }
}
