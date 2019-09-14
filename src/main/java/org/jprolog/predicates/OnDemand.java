// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.predicates;

import org.jprolog.execution.Environment;

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
