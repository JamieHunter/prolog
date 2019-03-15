// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.exceptions.PrologInstantiationError;
import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.InstructionPointer;
import prolog.execution.LocalContext;
import prolog.execution.RestoresLocalContext;
import prolog.expressions.Term;

/**
 * Indirect call to a predicate, with cut behavior modification. This is further overridden for special once-like
 * variants.
 */
public class ExecCall implements Instruction {
    protected final Environment environment;
    protected final Term callTerm;
    private final Instruction precompiled;

    /**
     * Construct a call bound to a given Environment with a partially resolved term.
     *
     * @param environment Execution environment.
     * @param callTerm    Term to call. Not assumed to be grounded, might be a variable.
     */
    public ExecCall(Environment environment, Term callTerm) {
        this.environment = environment;
        this.callTerm = callTerm;
        if (callTerm.isInstantiated()) {
            // Create nested context to wrap in a cut-scope
            CompileContext compiling = new CompileContext(environment);
            callTerm.compile(compiling);
            precompiled = compiling.toInstruction();
        } else {
            // defer compiling until run-time.
            precompiled = null;
        }
    }

    /**
     * Construct a call bound to a given Environment with a precompiled term.
     *
     * @param environment Execution environment.
     * @param precompiled Term to call
     */
    public ExecCall(Environment environment, Instruction precompiled) {
        this.environment = environment;
        this.callTerm = null;
        this.precompiled = precompiled;
    }

    /**
     * Invocation of call. If term was not precompiled, it is compiled now. The term is then executed in a constrained
     * scope. When overridden other rules may be applied.
     *
     * @param environment Execution environment
     */
    @Override
    public void invoke(Environment environment) {
        Instruction nested = precompiled;
        if (nested == null) {
            Term bound = callTerm.resolve(environment.getLocalContext());
            if (bound.isInstantiated()) {
                // Need to compile on the fly
                CompileContext compiling = new CompileContext(environment);
                bound.compile(compiling);
                nested = compiling.toInstruction();
            } else {
                throw PrologInstantiationError.error(environment, bound);
            }
        }
        preCall();
        // Execute code
        nested.invoke(environment);
    }

    /**
     * Called prior to setting up return address / cutScope. By default this adds a cut-constraining scope.
     */
    protected void preCall() {
        RestoreCutDepth ip = prepareCall(); // with side-effects
        if (environment.getIP() instanceof RestoresLocalContext) {
            // Tail-call elimination: Eliminate the "callIP" below, side effects still hold
        } else {
            // On return from precompiled, restore cut-scope we're about to set up
            environment.callIP(ip);
        }
    }

    /**
     * Build a context with side-effects.
     *
     * @return instance of RestoreCutDepth
     */
    protected RestoreCutDepth prepareCall() {
        LocalContext context = environment.getLocalContext();
        return new RestoreCutDepth(context);
    }

    /**
     * IP that restores cut point on "return"
     */
    protected static class RestoreCutDepth implements InstructionPointer {
        final LocalContext context;
        final int depth;

        RestoreCutDepth(LocalContext context) {
            this.context = context;
            this.depth = context.reduceCutDepth();
        }

        @Override
        public void next() {
            context.restoreCutDepth(depth);
            // drop "RestoreCutDepth"
            context.environment().restoreIP();
        }

        @Override
        public InstructionPointer copy() {
            return this;
        }
    }
}
