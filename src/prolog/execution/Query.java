// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

import prolog.bootstrap.Interned;
import prolog.expressions.Term;
import prolog.instructions.DeferredCallInstruction;
import prolog.instructions.ExecDefer;
import prolog.library.Control;
import prolog.predicates.Predication;

/**
 * Main entry point into the interpreter. Compile and execute a query.
 */
public class Query {
    protected final Environment environment;
    protected final LocalContext context;
    private Instruction instruction = Control.TRUE;

    /**
     * Construct a new query associated with environment.
     *
     * @param environment Execution environment.
     */
    public Query(Environment environment) {
        this.environment = environment;
        this.context = new LocalContext(environment,
                new Predication(Interned.QUERY_FUNCTOR, 1),
                CutPoint.TERMINAL
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
     * Compile a query to prepare for execution. Note, this resets any environment state.
     *
     * @param term Callable term to compile.
     */
    public void compile(Term term) {
        environment.reset();
        environment.setLocalContext(context); // establish for purpose of compiling and exceptions
        environment.setCutPoint(context); // related cut point
        instruction = new ExecDefer(new DeferredCallInstruction(term));
    }

    /**
     * Execute run-loop. Behavior may be modified with the onSuccess/onFailed handlers. Note, this resets the
     * environment state.
     *
     * @return true on success, false otherwise
     */
    public ExecutionState run() {
        reset();
        ExecutionState state;
        do {
            state = cycle();
        } while(!state.isTerminal());
        return state;
    }

    public void reset() {
        environment.reset();
        environment.setLocalContext(context);
        environment.setCutPoint(context);
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
