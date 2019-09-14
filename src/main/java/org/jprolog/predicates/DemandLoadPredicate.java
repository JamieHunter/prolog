// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.predicates;

import org.jprolog.expressions.CompoundTerm;
import org.jprolog.execution.CompileContext;
import org.jprolog.execution.Environment;
import org.jprolog.utility.TrackableList;

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
     * {@inheritDoc}
     */
    @Override
    public boolean isDiscontiguous() {
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
        // Demand-load at compile time, give the demand-load process its own environment
        Environment environment = new Environment(context.environmentShared());
        // What is current definition? Have we already loaded it?
        PredicateDefinition defn = environment.lookupPredicate(predication);
        if (defn == this) {
            // this step happening before load prevents cycles
            defn = environment.createDictionaryEntry(predication);
            // load definition
            onDemand.load(environment);
        }
        // delegate compile step to new clause
        defn.compile(predication, context, term);
    }
}
