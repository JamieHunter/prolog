// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.execution;

import org.jprolog.predicates.Predication;
import org.jprolog.variables.ActiveVariable;
import org.jprolog.variables.LabeledVariable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Local context for a specific instantiation of a specific clause. Note that this context will exist in Java memory
 * for as long as there is potential of any inactive labeled variables, specifically backtrack entries referencing this state.
 */
public final class LocalContext {

    private final Environment environment;
    private final Map<Long, ActiveVariable> variables = new HashMap<>();
    private final Predication predication;

    /**
     * Construct a local context
     *
     * @param environment Execution environment
     * @param predication Predication to report on error (effectively stack entry)
     */
    LocalContext(Environment environment, Predication predication) {
        this.environment = environment;
        this.predication = predication;
    }

    /**
     * @return execution environment
     */
    public Environment environment() {
        return environment;
    }

    /**
     * @return predication (effectively stack entry).
     */
    public Predication getPredication() {
        return predication;
    }

    /**
     * Return an active variable for an inactive labeled variable. Note that it is permissible for two variables to exist with
     * the same name as long as they have different unique id's that were assigned at the time the term was read.
     * The Prolog spec describes that a term is "copied" on execution. This implementation defers that copy.
     *
     * @param var Labeled inactive variable.
     * @return associated activate variable.
     */
    public ActiveVariable copy(LabeledVariable var) {
        return variables.computeIfAbsent(var.id(), i -> new ActiveVariable(environment, var.name(), environment.nextVariableId()));
    }

    /**
     * Creates a view map from the set of variables, giving each variable a unique name.
     *
     * @return Map of named variables
     */
    public Map<String, ActiveVariable> retrieveVariableMap() {
        TreeMap<String, ActiveVariable> sortedVars = new TreeMap<>();
        for (ActiveVariable var : variables.values()) {
            String n = var.name();
            long id = var.id();
            if (n.equals("_") || sortedVars.containsKey(n)) {
                n = n + id;
            }
            sortedVars.put(n, var);
        }
        return Collections.unmodifiableMap(sortedVars);
    }
}
