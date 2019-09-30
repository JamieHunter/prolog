// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.predicates;

import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.Term;
import org.jprolog.instructions.ClauseEntryBodyInstruction;
import org.jprolog.unification.Unifier;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.utility.LinkNode;

/**
 * An entry for a given clause. The clause is associated with a {@link Environment.Shared}
 * context, and is not compiled until an instruction is obtained. The clause may be compiled or recompiled depending
 * on debug context / global changes (broadGeneration).
 */
public class ClauseEntry {
    private final CompoundTerm head;
    private final Term body;
    private final Unifier unifier;
    private final ClauseEntryBodyInstruction instruction;
    private final LinkNode<ClauseEntry> node;

    /**
     * Create a clause entry.
     *
     * @param head        Head term (for reference)
     * @param body        Callable body term (for reference)
     * @param unifier     Head unifier
     */
    public ClauseEntry(CompoundTerm head, Term body, Unifier unifier) {
        this.head = head;
        this.body = body;
        this.instruction = new ClauseEntryBodyInstruction(this);
        this.unifier = unifier;
        this.node = new LinkNode<>(this);
    }

    /**
     * Retrieve head term
     *
     * @return Head term
     */
    public CompoundTerm getHead() {
        return this.head;
    }

    /**
     * Retrieve body term
     *
     * @return Body term
     */
    public Term getBody() {
        return this.body;
    }

    /**
     * Retrieve head unifier
     *
     * @return Head unifier
     */
    public Unifier getUnifier() {
        return this.unifier;
    }

    /**
     * Retrieve body instruction
     *
     * @return instruction
     */
    public Instruction getInstruction() {
        return instruction;
    }

    /**
     * Link node to link clauses together.
     *
     * @return link node
     */
    public LinkNode<ClauseEntry> getNode() {
        return this.node;
    }
}
