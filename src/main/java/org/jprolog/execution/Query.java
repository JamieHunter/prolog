// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.execution;

import org.jprolog.bootstrap.Interned;
import org.jprolog.cuts.ClauseCutBarrier;
import org.jprolog.cuts.CutPoint;
import org.jprolog.expressions.Term;
import org.jprolog.instructions.ExecBlock;
import org.jprolog.instructions.ExecDefer;
import org.jprolog.library.Control;
import org.jprolog.predicates.Predication;

/**
 * Main entry point into the interpreter. Compile and execute a query.
 */
public class Query {
    protected final Environment environment;
    protected final LocalContext context;
    protected final CutPoint initialCutPoint;
    private Instruction instruction = Control.TRUE;

    /**
     * Construct a new query associated with environment.
     *
     * @param environment Execution environment.
     */
    public Query(Environment environment) {
        this.environment = environment;
        this.initialCutPoint = environment.getCutPoint();
        this.context = new LocalContext(environment,
                new Predication(Interned.QUERY_FUNCTOR, 1)
        );
    }

    /**
     * Retrieve root local context specifically constructed for this query.
     *
     * @return Local Context
     */
    public LocalContext getLocalContext() {
        return context;
    }

    /**
     * Prepare an instruction to execute.
     *
     * @param term Callable term to execute in the future.
     */
    public void prepare(Term term) {
        instruction = new ExecDefer(
                // Don't execute at start()
                ExecBlock.callFuture(term)
        );
    }

    /**
     * Execute run-loop. Behavior may be modified with the onSuccess/onFailed handlers. Note, this resets the
     * environment state.
     *
     * @return true on success, false otherwise
     */
    public ExecutionState run() {
        start();
        ExecutionState state;
        do {
            state = cycle();
        } while (!state.isTerminal());
        return state;
    }

    public void reset() {
        environment.reset();
        environment.setLocalContext(context);
        environment.setCutPoint(initialCutPoint);
    }

    public void start() {
        reset();
        environment.setCutPoint(new ClauseCutBarrier(environment, initialCutPoint));
        instruction.invoke(environment);
    }

    public ExecutionState cycle() {
        ExecutionState state = environment.run();
        if (state == ExecutionState.SUCCESS) {
            state = onSuccess();
            if (state == ExecutionState.BACKTRACK) {
                environment.backtrack();
                return state;
            }
        } else {
            onFailed();
        }
        return state;
    }

    /**
     * Override to allow forced backtracking.
     *
     * @return true to force backtrack
     */
    protected ExecutionState onSuccess() {
        return ExecutionState.SUCCESS;
    }

    /**
     * Override to handle goal failure (no more solutions).
     */
    protected void onFailed() {
    }

}
