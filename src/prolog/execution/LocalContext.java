// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

import prolog.predicates.Predication;
import prolog.variables.BoundVariable;
import prolog.variables.UnboundVariable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Local context for a specific instantiation of a specific clause. Note that this context will exist in Java memory
 * for as long as any bound variables or backtrack entries reference this state.
 */
public final class LocalContext extends BasicCutPoint {

    private final Map<Long, BoundVariable> variables = new HashMap<>();
    private final Predication predication;

    /**
     * Construct a local context
     *
     * @param environment Execution environment
     * @param predication Predication to report on error (effectively stack entry)
     */
    LocalContext(Environment environment, Predication predication, CutPoint parentCutPoint) {
        super(environment, parentCutPoint);
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
     * Return a bound variable for an unbound variable. Note that it is permissible for two variables to exist with
     * the same name as long as they have different unique id's that were assigned at the time the term was read.
     * The bound variable however is unique to this context.
     *
     * @param var Unbound variable.
     * @return associated bound variable.
     */
    public BoundVariable bind(UnboundVariable var) {
        return bind(var.name(), var.id());
    }

    /**
     * Return a bound variable. Note that it is permissible for two variables to exist with
     * the same name as long as they have different unique id's that were assigned at the time the term was read.
     * The bound variable however is unique to this context.
     *
     * @param name Name of variable
     * @param id Id of variable
     * @return associated bound variable.
     */
    public BoundVariable bind(String name, long id) {
        return variables.computeIfAbsent(id, i -> new BoundVariable(this, name, id));
    }

    /**
     * Creates a view map from the set of variables, giving each variable a unique name.
     *
     * @return Map of named variables
     */
    public Map<String, BoundVariable> retrieveVariableMap() {
        TreeMap<String, BoundVariable> sortedVars = new TreeMap<>();
        for (BoundVariable var : variables.values()) {
            String n = var.name();
            long id = var.id();
            if (n.equals("_") || sortedVars.containsKey(n)) {
                n = n + String.valueOf(id);
            }
            sortedVars.put(n, var);
        }
        return Collections.unmodifiableMap(sortedVars);
    }
}
