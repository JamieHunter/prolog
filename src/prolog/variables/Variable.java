// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.variables;

import prolog.execution.Environment;
import prolog.expressions.Container;
import prolog.expressions.Term;
import prolog.expressions.TypeRank;

/**
 * Marker interface indicating this is a variable. Variables return names and each variable has
 * a unique id.
 */
public interface Variable extends Container {
    /**
     * Name of variable
     *
     * @return Name
     */
    String name();

    /**
     * Unique ID of variable
     *
     * @return ID
     */
    long id();

    /**
     * {@inheritDoc}
     */
    @Override
    default int typeRank() {
        return TypeRank.VARIABLE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default int compareSameType(Term o) {
        return Long.compare(id(), ((Variable)o).id());
    }
}
