// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.predicates;

import prolog.execution.Instruction;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.unification.Unifier;
import prolog.utility.LinkNode;

/**
 * An entry for a given clause.
 */
public class ClauseEntry {
    private final CompoundTerm head;
    private final Term body;
    private final Unifier unifier;
    private final Instruction instruction;
    private final LinkNode<ClauseEntry> node;

    /**
     * Create a clause entry.
     *
     * @param head        Head term (for reference)
     * @param body        Callable body term (for reference)
     * @param unifier     Head unifier
     * @param instruction Compiled instruction
     */
    public ClauseEntry(CompoundTerm head, Term body, Unifier unifier, Instruction instruction) {
        this.head = head;
        this.body = body;
        this.unifier = unifier;
        this.instruction = instruction;
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
        return this.instruction;
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
