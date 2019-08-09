// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.predicates;

import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.expressions.CompoundTerm;
import prolog.utility.TrackableList;

/**
 * A deferred predicate. If used, must load definitions and replace predicates with actual predicates.
 */
public class DemandLoadPredicate extends PredicateDefinition {

    private final TrackableList<ClauseEntry> clauses = new TrackableList<>();
    private final OnDemand onDemand;

    public DemandLoadPredicate(OnDemand onDemand) {
        this.onDemand = onDemand;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDynamic() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMultifile() {
        return false;
    }

    /**
     * During compile, actively replace demand-load predicate with clause-search predicate and load definitions from
     * source file.
     *
     * @param predication Predication used as a key to find predicate
     * @param context     Compile context
     * @param term        Compound term being compiled
     */
    @Override
    public void compile(Predication predication, CompileContext context, CompoundTerm term) {
        // Demand-load at compile time
        Environment environment = context.environment();
        // What is current definition? Have we already loaded it?
        PredicateDefinition defn = environment.lookupPredicate(predication);
        if (defn == this) {
            // this step happening before load prevents cycles
            defn = environment.createDictionaryEntry(predication);
            // load definition
            onDemand.load(context.environment());
        }
        // delegate compile step to new clause
        defn.compile(predication, context, term);
    }
}
