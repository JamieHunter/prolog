// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.predicates;

import prolog.execution.Environment;

/**
 * Interface to implement on-demand loader for on-demand predicate.
 */
public interface OnDemand {
    /**
     * Load one or more predicates.
     *
     * @param environment Execution environment
     */
    void load(Environment environment);
}
