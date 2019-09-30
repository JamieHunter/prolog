// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.enumerators;

import org.jprolog.execution.Environment;
import org.jprolog.variables.ActiveVariable;
import org.jprolog.variables.LabeledVariable;

/**
 * Context for copying a tree of terms
 */
public class CopyTerm extends CopySimpleTerm {

    public CopyTerm(Environment environment) {
        super(environment);
    }

    /**
     * Creates an active variable that is unique, but also reused across the term.
     *
     * @param source ActiveVariable to possibly rename
     * @return Renamed variable (active and referenced in current context)
     */
    @Override
    protected ActiveVariable renameVariable(ActiveVariable source) {
        return context.copy((LabeledVariable) super.renameVariable(source));
    }
}
