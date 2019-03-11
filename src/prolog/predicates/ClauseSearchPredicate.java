// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.predicates;

import prolog.execution.CompileContext;
import prolog.expressions.CompoundTerm;
import prolog.instructions.ExecRunClause;
import prolog.utility.TrackableList;

/**
 * A set of user-defined clauses that need to be searched
 * TODO: Optimize search process for large dictionaries
 */
public class ClauseSearchPredicate extends PredicateDefinition {

    private final TrackableList<ClauseEntry> clauses = new TrackableList<>();
    private static final ClauseEntry[] ELEMENT_ARRAY_TYPE = new ClauseEntry[0];
    private boolean isDynamic = false;

    /**
     * Create a new predicate
     */
    public ClauseSearchPredicate() {
    }

    /**
     * Used to support conditional inserts into map.
     *
     * @param predication Ignored
     */
    public ClauseSearchPredicate(Predication predication) {
    }

    /**
     * Enable dynamic inserts
     *
     * @param dynamic Dynamic flag
     */
    public void setDynamic(boolean dynamic) {
        this.isDynamic = dynamic;
    }

    /**
     * True if dynamic inserts are enabled.
     *
     * @return flag
     */
    @Override
    public boolean isDynamic() {
        return isDynamic;
    }

    /**
     * Compile an instruction to invoke this predicate.
     *
     * @param predication Predication describing this predicate.
     * @param context     Compiling context
     * @param term        Term to unify.
     */
    @Override
    public void compile(Predication predication, CompileContext context, CompoundTerm term) {
        context.add(new ExecRunClause(predication, this, term));
    }

    /**
     * Retrieve snapshot of all clauses.
     *
     * @return clauses
     */
    public ClauseEntry[] getClauses() {
        return clauses.elements(ELEMENT_ARRAY_TYPE);
    }

    /**
     * Add a new clause to head
     *
     * @param entry Entry to add
     */
    public void addStart(ClauseEntry entry) {
        clauses.addHead(entry.getNode());
    }

    /**
     * Add a new clause to tail
     *
     * @param entry Entry to add
     */
    public void addEnd(ClauseEntry entry) {
        clauses.addTail(entry.getNode());
    }
}
