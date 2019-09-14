package org.jprolog.variables;

import org.jprolog.execution.Backtrack;
import org.jprolog.execution.Environment;

import java.util.ListIterator;

/**
 * Corefers two variables, which could also be Coreference variables. Assumes variables
 * implement VariableBase.
 */
/*package*/
class Coreference extends VariableBase {
    private final Environment environment;
    private final VariableBase left;
    private final VariableBase right;
    private Long id = null; // as needed

    Coreference(VariableBase left, VariableBase right) {
        // Left and right here is arbitrary.
        this.left = left;
        this.right = right;
        this.environment = left.environment();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return left.name() + "_" + right.name();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long id() {
        if (id == null) {
            // only allocate ID when needed
            id = environment.nextVariableId();
        }
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Environment environment() {
        return environment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDeterministic() {
        return left.isDeterministic() && right.isDeterministic();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pushBacktrack() {
        if (isDeterministic()) {
            return;
        }
        environment.pushBacktrack(new Backtrack() {
            @Override
            public void undo() {
                // undo the co-referenced value
                value = null;
            }

            @Override
            public void cut(ListIterator<Backtrack> iter) {
                // only cut if both left and right are deterministic
                if (isDeterministic()) {
                    iter.remove();
                }
            }
        });
    }
}
