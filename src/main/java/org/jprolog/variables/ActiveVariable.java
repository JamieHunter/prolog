// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.variables;

import org.jprolog.enumerators.EnumTermStrategy;
import org.jprolog.execution.Backtrack;
import org.jprolog.execution.CompileContext;
import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.Term;
import org.jprolog.instructions.ExecBlock;
import org.jprolog.instructions.ExecFuture;
import org.jprolog.instructions.ExecCall;
import org.jprolog.io.WriteContext;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ListIterator;

/**
 * Represents an activated variable (that can be instantiated).
 */
public class ActiveVariable implements Variable {

    private final Environment environment;
    // Name of variable
    private final String name;
    // Unique ID of variable to disambiguate two different variables of same name, note that active variables
    // have an ID that is unique across the entire environment
    private final long id;
    // Value. In the case of co-reference variables, this is a unique variable, recursive to any depth
    private Term value = null;
    // If a labeled variable is computed, it is cached here
    private WeakReference<LabeledVariable> label = null;


    /**
     * Create a active variable.
     *
     * @param environment Execution environment
     * @param name        Name of this variable from previous call to read
     * @param id          Unique identifier of variable
     */
    public ActiveVariable(Environment environment, String name, long id) {
        this.environment = environment;
        this.name = name;
        this.id = id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (value == null) {
            return "(" + name() + "_" + id() + ")";
        } else {
            return value.toString();
        }
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
    public long corefId() {
        // effective id after resolving coreferences
        Term v = value;
        while (v instanceof ActiveVariable) {
            v = ((ActiveVariable) v).value;
        }
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term value() {
        if (value == null) {
            return this;
        } else {
            return value.value();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term resolve(LocalContext context) {
        if (value == null) {
            return this;
        } else {
            return value.resolve(context);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term enumTerm(EnumTermStrategy strategy) {
        if (value == null) {
            return strategy.visitVariable(this);
        } else {
            return value.enumTerm(strategy);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LabeledVariable label() {
        LabeledVariable label = this.label != null ? this.label.get() : null;
        if (label == null) {
            this.label = new WeakReference<>(label = new LabeledVariable(name, id));
        }
        return label;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGrounded() {
        return isInstantiated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void compile(CompileContext compiling) {
        // A compiled variable is considered the same as compiling 'call'
        // This case can happen, e.g. call(once(X)), X is resolved in the call context,
        // and infers a call of an active variable for once()
        compiling.addCall(this, new ExecCall(
                ExecBlock.future(this)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInstantiated() {
        return value != null && value.isInstantiated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(WriteContext context) throws IOException {
        context.write(name()); // TODO, need to follow write mode
    }

    /**
     * {@inheritDoc}
     */
    private void pushBacktrack() {
        environment.pushBacktrackIfNotDeterministic(id, new Backtrack() {
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
                iter.remove();
            }
        });
    }

    /**
     * Instantiate variable if variable is not yet instantiated.
     *
     * @param newValue New value to assign to variable
     * @return true if considered instantiated / exactly the same, false if further unification comparison required.
     */
    @Override
    public boolean instantiate(Term newValue) {
        Term tipThis = value();
        if (tipThis != this) {
            // delegate
            return tipThis.instantiate(newValue);
        }
        Term tipOther = newValue.value();
        if (tipOther.isInstantiated()) {
            value = tipOther;
            pushBacktrack();
            return true;
        }
        if (!((Variable) tipOther).isActive()) {
            throw new InternalError("Incorrect state attempting to assign inactive variable " + tipOther.toString() + " to " + toString());
        }
        ActiveVariable varOther = (ActiveVariable) tipOther;
        // introduce new independent variable for co-reference assignment
        long id = environment.nextVariableId();
        ActiveVariable coref = new ActiveVariable(environment, "_G" + id, id);
        value = coref;
        pushBacktrack();
        varOther.value = coref;
        varOther.pushBacktrack();
        return true;
    }
}
