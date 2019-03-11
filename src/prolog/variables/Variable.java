// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.variables;

import prolog.execution.Environment;
import prolog.expressions.Container;

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
}
