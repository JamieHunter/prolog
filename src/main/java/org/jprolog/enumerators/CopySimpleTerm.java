// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.enumerators;

import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.Term;
import org.jprolog.variables.ActiveVariable;
import org.jprolog.variables.LabeledVariable;
import org.jprolog.variables.Variable;

import java.util.HashMap;

/**
 * Context for copying a tree of terms
 */
public class CopySimpleTerm extends EnumTermStrategy {

    protected final LocalContext context;
    private final HashMap<Long, Variable> renamed = new HashMap<>();

    public CopySimpleTerm(Environment environment) {
        super(environment);
        this.context = environment.getLocalContext();
    }

    /**
     * Any variable is replaced with an actively relabled variable.
     *
     * @param variable Variable reference
     * @return relabeld variable.
     */
    @Override
    public Term visitVariable(Variable variable) {
        return computeUncachedTerm(variable, t -> checkAndRenameVariable(variable));
    }

    /**
     * Ensures variable is renamed, and correctly handles a variable that is currently simply a label before
     * renaming.
     *
     * @param variable Variable to rename
     * @return renamed variable.
     */
    private Variable checkAndRenameVariable(Variable variable) {
        ActiveVariable av;
        if (variable.isActive()) {
            av = (ActiveVariable) variable;
        } else {
            // unlikely to hit this path, but make sure variables are mapped via context first
            av = context.copy((LabeledVariable) variable);
        }
        // copy mapping ensuring variable is renamed
        return renamed.computeIfAbsent(av.id(),
                i -> renameVariable(av));
    }

    /**
     * Creates a labeled variable that is unique, but also reused across the term.
     *
     * @param source ActiveVariable to possibly rename
     * @return Renamed variable
     */
    protected Variable renameVariable(ActiveVariable source) {
        return new LabeledVariable(source.name(), environment().nextVariableId());
    }
}
