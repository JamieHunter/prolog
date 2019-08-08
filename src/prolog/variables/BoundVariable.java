// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.variables;

import prolog.execution.Backtrack;
import prolog.execution.Environment;
import prolog.execution.LocalContext;

import java.util.ListIterator;

/**
 * Represents a variable bound to a local context
 */
public class BoundVariable extends VariableBase {

    // Name of variable
    private final String name;
    // Unique ID of variable to disambiguate two different variables of same name
    private final long id;
    // Local context variable is bound to
    private final LocalContext context;

    /**
     * Create a new context-bound variable.
     *
     * @param context Local Context that this variable is bound to
     * @param name    Name of this variable from previous call to read
     * @param id      Unique identifier of variable
     */
    public BoundVariable(LocalContext context, String name, long id) {
        this.name = name;
        this.id = id;
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Environment environment() {
        return context.environment();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDeterministic() {
        return context.isDeterministic();
    }

    /**
     * Retrieve name of variable
     *
     * @return Variable name
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Retrieve unique identifier of variable
     *
     * @return Unique ID
     */
    @Override
    public long id() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void pushBacktrack() {
        if (!context.isDeterministic()) {
            environment().pushBacktrack(new Backtrack() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void undo() {
                    // Undo is trivial.
                    value = null;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void cut(ListIterator<Backtrack> iter) {
                    if (context.isDeterministic()) {
                        // The entry can be erased, with consideration needed for co-reference variables
                        iter.remove();
                    }
                }
            });
        }
    }
}
