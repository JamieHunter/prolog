// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

import prolog.execution.LocalContext;

/**
 * Capture instruction context and local context
 */
public class Scoped {
    public final InstructionContext instructionContext;
    public final LocalContext localContext;
    private int iteration = 0;

    public static final Scoped NULL = new Scoped(InstructionContext.NULL, null);

    public Scoped(InstructionContext instructionContext, LocalContext localContext) {
        this.instructionContext = instructionContext;
        this.localContext = localContext;
    }

    public int getIteration() {
        return this.iteration;
    }

    public void incrementIteration() {
        this.iteration++;
    }
}
