// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.bootstrap.Interned;
import prolog.constants.Atomic;
import prolog.exceptions.FuturePrologError;
import prolog.exceptions.PrologError;
import prolog.exceptions.PrologInstantiationError;
import prolog.exceptions.PrologTypeError;
import prolog.execution.BasicCutPoint;
import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.InstructionPointer;
import prolog.execution.RestoresLocalContext;
import prolog.expressions.CompoundTerm;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;
import prolog.expressions.TermList;

import java.util.ArrayList;
import java.util.List;

/**
 * Indirect call to a predicate, with cut behavior modification. This is further overridden for special once-like
 * variants.
 */
public class ExecCall implements Instruction {
    protected final Environment environment;
    protected final Term callTerm;
    protected final Term args;
    private final Instruction precompiled;

    /**
     * Construct a call bound to a given Environment with a partially resolved term split into
     * functor and arguments
     *
     * @param environment Execution environment.
     * @param callTerm    Term to call. Not assumed to be grounded, might be a variable.
     */
    public ExecCall(Environment environment, Term callTerm) {
        this.environment = environment;
        this.callTerm = callTerm;
        this.args = null;
        Instruction precompiled = null;
        if (callTerm.isInstantiated()) {
            // Attempt to compile.
            // Note, if compiling cannot be performed, or it fails, don't fail here,
            // by contract of Call, the failure is deferred until runtime.
            CompileContext compiling = new CompileContext(environment);
            try {
                callTerm.compile(compiling);
                precompiled = compiling.toInstruction();
            } catch (PrologError | FuturePrologError e) {
                // See sec78.pl catch_test(3)
            }
        }
        this.precompiled = precompiled;
    }

    /**
     * Construct a call bound to a given Environment with a partially resolved term.
     *
     * @param environment Execution environment.
     * @param callTerm    Term to call. Not grounded, might be a variable, cannot be compound.
     * @param args        Arguments to be merged with functor, not assumed to be grounded.
     */
    public ExecCall(Environment environment, Term callTerm, Term args) {
        this.environment = environment;
        this.callTerm = callTerm;
        this.args = args;
        precompiled = null;
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
        this.args = null;
        this.precompiled = precompiled;
    }

    private Term apply(Environment environment, Term bound, Term args) {
        Atomic functor;
        List<Term> members = new ArrayList<>();
        if (bound instanceof CompoundTerm) {
            CompoundTerm compBound = (CompoundTerm) bound;
            functor = compBound.functor();
            for (int i = 0; i < compBound.arity(); i++) {
                members.add(compBound.get(i));
            }
        } else if (bound.isAtom()) {
            functor = (Atomic) bound;
        } else {
            throw PrologTypeError.callableExpected(environment, bound);
        }
        if (CompoundTerm.termIsA(args, Interned.CALL_FUNCTOR)) {
            CompoundTerm callArg = (CompoundTerm) args;
            for (int i = 0; i < callArg.arity(); i++) {
                members.add(callArg.get(i));
            }
        } else if (CompoundTerm.termIsA(args, Interned.LIST_FUNCTOR)) {
            List<Term> list = TermList.extractList(args);
            members.addAll(list);
        } else {
            throw PrologTypeError.listExpected(environment, args);
        }
        return new CompoundTermImpl(functor, members);
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
            if (!bound.isInstantiated()) {
                throw PrologInstantiationError.error(environment, bound);
            }
            if (args != null) {
                Term boundArgs = args.resolve(environment.getLocalContext());
                if (!boundArgs.isInstantiated()) {
                    throw PrologInstantiationError.error(environment, boundArgs);
                }
                bound = apply(environment, bound, boundArgs);
            }
            // Compile on the fly
            try {
                CompileContext compiling = new CompileContext(environment);
                bound.compile(compiling);
                nested = compiling.toInstruction();
            } catch (PrologError | FuturePrologError e) {
                // friendly (and per spec) error message with full term
                // assume any error during compile phase amounts to this term not being callable
                // TODO, this fails sec78.pl test "error_test(call((write(3), 1)), type_error(callable, 1))"
                // based on other examples, is the test at fault?
                //throw PrologTypeError.callableExpected(environment, bound);
                throw e;
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
        ConstrainedCutPoint ip = prepareCall(); // with side-effects
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
     * @return instance of ConstrainedCutPoint
     */
    protected ConstrainedCutPoint prepareCall() {
        return new ConstrainedCutPoint(environment);
    }

    /**
     * This class acts as a localized cut-point, and an IP that restores the original cut point
     */
    protected static class ConstrainedCutPoint extends BasicCutPoint implements InstructionPointer {
        ConstrainedCutPoint(Environment environment) {
            super(environment, environment.getCutPoint());
            environment.setCutPoint(this);
        }

        @Override
        public boolean isDeterministic() {
            return super.isDeterministic() && parent.isDeterministic();
        }

        @Override
        public void next() {
            environment.setCutPoint(parent);
            environment.restoreIP();
        }

        @Override
        public InstructionPointer copy() {
            return this;
        }
    }
}
