// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

import prolog.execution.LocalContext;

import java.lang.ref.WeakReference;

/**
 * Capture instruction context and local context
 */
public class Scoped {
    private final WeakReference<InstructionContext> instructionContext;
    private final WeakReference<LocalContext> localContext;
    public final boolean traceable;
    private int iteration = 0;
    private final long seqId;

    public static final Scoped NULL = new Scoped(InstructionContext.NULL, null, false, 0L);

    public Scoped(InstructionContext instructionContext, LocalContext localContext, boolean traceable, long seqId) {
        // Weak references to avoid reference loops for WeakHashMap
        this.instructionContext = new WeakReference<>(instructionContext);
        this.localContext = localContext == null ? null : new WeakReference<>(localContext);
        this.traceable = traceable;
        this.seqId = seqId;
    }

    public InstructionContext instructionContext() {
        InstructionContext ctx = instructionContext.get();
        if (ctx == null) {
            ctx = InstructionContext.NULL;
        }
        return ctx;
    }

    public LocalContext localContext() {
        if (localContext == null) {
            return null;
        } else {
            return localContext.get();
        }
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
