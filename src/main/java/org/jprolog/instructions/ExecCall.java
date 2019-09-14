// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.execution.BasicCutPoint;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.execution.InstructionPointer;
import org.jprolog.execution.RestoresLocalContext;

/**
 * Indirect call to a predicate, with cut behavior modification. This is further overridden for special once-like
 * variants.
 */
public class ExecCall implements Instruction {
    private final Instruction instruction;

    /**
     * Construct a "call" which manages cut scoping
     *
     * @param precompiled Term to call
     */
    public ExecCall(Instruction precompiled) {
        this.instruction = precompiled;
    }

    /**
     * Invocation of call. If term was not precompiled, it is compiled now. The term is then executed in a constrained
     * scope. When overridden other rules may be applied.
     *
     * @param environment Execution environment
     */
    @Override
    public void invoke(Environment environment) {
        preCall(environment);
        instruction.invoke(environment);
    }

    /**
     * @param environment Execution environment
     *                    Called prior to setting up return address / cutScope. By default this adds a cut-constraining scope.
     */
    protected void preCall(Environment environment) {
        ConstrainedCutPoint ip = prepareCall(environment); // with side-effects
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
     * @param environment Execution environment
     * @return instance of ConstrainedCutPoint
     */
    protected ConstrainedCutPoint prepareCall(Environment environment) {
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
