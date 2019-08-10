package prolog.variables;

import prolog.execution.CompileContext;
import prolog.execution.CopyTermContext;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.expressions.Term;
import prolog.instructions.ExecCall;
import prolog.io.WriteContext;

import java.io.IOException;

/**
 * Base variable implementation shared between Coreference and BoundVariable
 */
/*package*/
abstract class VariableBase implements Variable {
    protected Term value = null;

    /**
     * Environment associated with variable
     */
    protected abstract Environment environment();

    /**
     * Record backtracking entry
     */
    protected abstract void pushBacktrack();

    /**
     * Determine if the paths to variable are deterministic.
     * @return true if deterministic
     */
    protected abstract boolean isDeterministic();

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
     * {@inheritDoc}
     */
    @Override
    public Term value(Environment environment) {
        if (value != null) {
            return value.value(environment);
        } else {
            return this;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term resolve(LocalContext context) {
        if (value != null) {
            return value.resolve(context);
        } else {
            return this;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term simplify(Environment environment) {
        if (value == null) {
            return new UnboundVariable(name(), id());
        } else {
            return value.simplify(environment);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term copyTerm(CopyTermContext context) {
        return context.copy(this, t -> {
            if (value == null) {
                // this is where copyTerm is distinct from simplify
                return context.var(name(), id());
            } else {
                return value.copyTerm(context);
            }
        });
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
    public boolean isInstantiated() {
        return value != null && value.isInstantiated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void compile(CompileContext compiling) {
        // A compiled variable is considered the same as compiling 'call'
        compiling.add(new ExecCall(compiling.environment(), value(compiling.environment())));
    }


    /**
     * Instantiate variable if variable is not yet instantiated.
     *
     * @param newValue New value to assign to variable
     * @return true if considered instantiated / exactly the same, false if further unification comparison required.
     */
    @Override
    public boolean instantiate(Term newValue) {
        //
        // newValue will have had .value() called on it before this point
        //
        if (newValue == this || newValue == value) {
            return true;
        }
        if (value == null) {
            if (newValue.isInstantiated()) {
                value = newValue;
                pushBacktrack();
            } else {
                VariableBase otherVar = (VariableBase)newValue;
                Coreference coreference = new Coreference(this, otherVar);
                value = coreference;
                pushBacktrack();
                otherVar.value = coreference;
                otherVar.pushBacktrack();
            }
            return true;
        } else {
            return value.instantiate(newValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(WriteContext context) throws IOException {
        context.write(name()); // TODO, need to follow write mode
    }
}
