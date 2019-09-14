// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.predicates;

import org.jprolog.expressions.CompoundTerm;
import org.jprolog.instructions.ExecRunClause;
import org.jprolog.debugging.DebugInstruction;
import org.jprolog.execution.CompileContext;
import org.jprolog.utility.TrackableList;

import java.util.ListIterator;

/**
 * A set of user-defined clauses that need to be searched
 * TODO: Optimize search process for large dictionaries
 */
public class ClauseSearchPredicate extends PredicateDefinition {

    private final TrackableList<ClauseEntry> clauses = new TrackableList<>();
    private static final ClauseEntry[] ELEMENT_ARRAY_TYPE = new ClauseEntry[0];
    private boolean isDynamic = false;
    private boolean isMultifile = false;
    private boolean isDiscontiguous = false;
    private LoadGroup loadGroup = null;

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
    public ClauseSearchPredicate(Predication.Interned predication) {
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
     * Inhibit loadgroup semantics
     *
     * @param multifile Multifile flag
     */
    public void setMultifile(boolean multifile) {
        this.isMultifile = multifile;
    }

    /**
     * Allow reference before definition / behave like dynamic?
     *
     * @param discontiguous Discontiguous definitions
     */
    public void setDiscontiguous(boolean discontiguous) {
        this.isDiscontiguous = discontiguous;
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
     * True if multifile behavior is enabled.
     *
     * @return flag
     */
    @Override
    public boolean isMultifile() {
        return isMultifile;
    }

    /**
     * True if discontiguous behavior is enabled.
     *
     * @return flag
     */
    @Override
    public boolean isDiscontiguous() {
        return isDiscontiguous;
    }


    public void changeLoadGroup(LoadGroup loadGroup) {
        if (isDynamic || isMultifile) {
            return; // does not apply
        }
        if (this.loadGroup != loadGroup) {
            ListIterator<ClauseEntry> it = clauses.listIterator();
            while(it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        this.loadGroup = loadGroup;
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
        // always wrap in debugger instruction, the overhead should be minimal
        ExecRunClause inst = new ExecRunClause(predication, this, term);
        context.add(term, new DebugInstruction(term, inst, true));
    }

    /**
     * Predicate is considered current if it has at least one clause defined.
     * @return true if at least one clause defined.
     */
    @Override
    public boolean isCurrentPredicate() {
        return !clauses.isEmpty();
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
