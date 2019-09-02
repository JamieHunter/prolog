// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.expressions.Term;
import prolog.predicates.ClauseEntry;

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
