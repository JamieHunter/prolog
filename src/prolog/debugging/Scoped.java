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
    public final boolean traceable;
    private int iteration = 0;
    private final long seqId;

    public static final Scoped NULL = new Scoped(InstructionContext.NULL, null, false, 0L);

    public Scoped(InstructionContext instructionContext, LocalContext localContext, boolean traceable, long seqId) {
        this.instructionContext = instructionContext;
        this.localContext = localContext;
        this.traceable = traceable;
        this.seqId = seqId;
    }

    public int getIteration() {
        return this.iteration;
    }

    public void incrementIteration() {
        this.iteration++;
    }

    /**
     * Time gives a chronological sense of what order an entry was visited. This allows the concept of exiting out etc
     * by working with 'time' rather than location.
     * @return time
     */
    public long getSeqId() {
        return seqId;
    }
}
