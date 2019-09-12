package prolog.enumerators;

import prolog.execution.Environment;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.variables.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * used with {@link prolog.instructions.ExecBagOf} etc to identify all free variables.
 */
public class VariableCollector extends EnumTermStrategy {

    protected Mode mode;
    protected final ArrayList<Term> variables = new ArrayList<>(); // in collection order

    public VariableCollector(Environment environment, Mode mode) {
        super(environment);
        this.mode = mode;
    }

    /**
     * Change operating mode (see {@link prolog.instructions.ExecBagOf}).
     * @param mode New mode
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /**
     * Compound terms are enumerated not recomputed
     * @param compound Compound term being visited
     * @return self
     */
    @Override
    public CompoundTerm visitCompoundTerm(CompoundTerm compound) {
        return compound.enumCompoundTermMembers(this);
    }

    /**
     * @return List of variables.
     */
    public List<Term> getVariables() {
        return variables;
    }

    /**
     * Visit and collect variables
     * @param source Source variable
     * @return source
     */
    @Override
    public Variable visitVariable(Variable source) {
        boolean seen = hasVariable(source);
        addVariable(source);
        if (mode == Mode.COLLECT && !seen) {
            variables.add(source);
        }
        return source;
    }

    public enum Mode {
        COLLECT,
        IGNORE
    }
}
