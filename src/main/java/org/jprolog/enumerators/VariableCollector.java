package org.jprolog.enumerators;

import org.jprolog.execution.Environment;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.instructions.ExecBagOf;
import org.jprolog.variables.Variable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * used with {@link ExecBagOf} etc to identify all free variables.
 */
public class VariableCollector extends EnumTermStrategy {

    protected Mode mode;
    protected final LinkedHashMap<Long, Optional<? extends Variable>> variables = new LinkedHashMap<>();

    public VariableCollector(Environment environment, Mode mode) {
        super(environment);
        this.mode = mode;
    }

    /**
     * Change operating mode (see {@link ExecBagOf}).
     *
     * @param mode New mode
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /**
     * Compound terms are enumerated not recomputed
     *
     * @param compound Compound term being visited
     * @return self
     */
    @Override
    public CompoundTerm visitCompoundTerm(CompoundTerm compound) {
        return compound.enumCompoundTermMembers(this);
    }

    /**
     * @return Collection of variables.
     */
    public List<? extends Variable> getVariables() {
        return variables.values().stream().
                filter(t -> t.isPresent())
                .map(t -> t.get())
                .collect(Collectors.toList());
    }

    /**
     * Visit and collect variables
     *
     * @param source Source variable
     * @return source
     */
    @Override
    public Variable visitVariable(Variable source) {
        variables.putIfAbsent(source.id(),
                mode == Mode.COLLECT ? Optional.of(source) : Optional.empty());
        return source;
    }

    public enum Mode {
        COLLECT,
        IGNORE
    }
}
