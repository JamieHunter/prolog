// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.debugging;

import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.expressions.CompoundTerm;

/**
 * Decorates an instruction that can be stepped in the debugger.
 */
public class DebugInstruction implements Instruction {

    private final CompoundTerm source;
    private final Instruction instruction;
    private final boolean traceable;

    public DebugInstruction(CompoundTerm source, Instruction instruction, boolean traceable) {
        this.source = source;
        this.instruction = instruction;
        this.traceable = traceable;
    }

    @Override
    public void invoke(Environment environment) {
        // via debugger
        environment.debugger().invoke(environment, this, instruction);
    }

    public CompoundTerm getSource() {
        return source;
    }

    public boolean isTraceable() {
        return traceable;
    }
}
