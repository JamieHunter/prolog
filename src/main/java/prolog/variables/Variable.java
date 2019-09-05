// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.variables;

import prolog.expressions.Container;
import prolog.expressions.Term;
import prolog.expressions.TypeRank;

import java.util.Set;

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
     * @return co-reference ID to use, or original ID
     */
    long corefId();

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
        // It might seem to make sense to compare only the id's. However if the variables are co-references, a
        // more relevant id may be buried, and the two id's are the same but for different contexts. The co-ref
        // id is therefore considered to be the significant id in the chain
        return Long.compare(corefId(), ((Variable) o).corefId());
    }
}
