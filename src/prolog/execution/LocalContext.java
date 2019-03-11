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
public final class LocalContext {

    static final int DETERMINISTIC = -1;
    private final Environment environment;
    private final Map<Long, BoundVariable> variables = new HashMap<>();
    private final Predication predication;
    private int cutDepth = DETERMINISTIC;

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
     * Return a bound variable for an unbound variable. Note that it is permissible for two variables to exist with
     * the same name as long as they have different unique id's that were assigned at the time the term was read.
     * The bound variable however is unique to this context.
     *
     * @param var Unbound variable.
     * @return associated bound variable.
     */
    public BoundVariable bind(UnboundVariable var) {
        final String name = var.name();
        final long id = var.id();
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

    /**
     * Indicates if execution within the clause is deterministic, that is, there will be no backtracking.
     *
     * @return true if deterministic
     */
    public boolean isDeterministic() {
        return cutDepth == DETERMINISTIC;
    }

    /**
     * If a cut occurs, cut does not go past this level. Call this method to get the current depth. If depth is
     * DETERMINISTIC, optimizations can occur. This method is used to retrieve the current cut depth.
     *
     * @return current cut depth to later restore using {@link #setCutDepth(int)}.
     */
    public int getCutDepth() {
        return cutDepth;
    }

    /**
     * Override (restore) cut depth from prior call to {@link #getCutDepth()}.
     *
     * @param depth Cut depth to restore.
     */
    void setCutDepth(int depth) {
        this.cutDepth = depth;
    }

    /**
     * Called for call etc where the cut depth is temporarily reduced.
     *
     * @return old depth to restore using {@link #restoreCutDepth(int)}.
     */
    public int reduceCutDepth() {
        if (cutDepth == DETERMINISTIC) {
            // so far execution has been deterministic and can be assumed to still be deterministic.
            return DETERMINISTIC;
        } else {
            // reduced cut depth to scope (determined by caller).
            int oldDepth = cutDepth;
            cutDepth = environment.getBacktrackDepth();
            return oldDepth;
        }
    }

    /**
     * Called at end of call, to restore cut depth.
     *
     * @param savedDepth Value returned by {@link #reduceCutDepth()}.
     */
    public void restoreCutDepth(int savedDepth) {
        // effectively min(cutDepth, savedDepth) when both have a depth specified,
        // else whichever has a depth specified
        if (savedDepth != DETERMINISTIC) {
            cutDepth = savedDepth;
        }
    }

    /**
     * Add a backtracking entry to the stack, moving this context out of being deterministic. CutDepth is set to
     * immediately before the decision point if it was deterministic.
     *
     * @param decisionPoint New decision point
     */
    public void pushDecision(DecisionPoint decisionPoint) {
        if (cutDepth == DETERMINISTIC) {
            cutDepth = environment.getBacktrackDepth();
        }
        environment.pushBacktrack(decisionPoint);
    }

    /**
     * Add backtracking entry to the stack, only if context is no longer deterministic
     *
     * @param backtrack Backtracking information
     */
    public void pushContextBacktrack(Backtrack backtrack) {
        if (cutDepth != DETERMINISTIC) {
            environment.pushBacktrack(backtrack);
        }
    }
}
