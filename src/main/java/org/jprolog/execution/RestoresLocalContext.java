// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.execution;

import org.jprolog.callstack.ImmutableExecutionPoint;

/**
 * This is an immutable execution point that we can assume restores LocalContext and Scope. This allows
 * tail-call elimination.
 */
public interface RestoresLocalContext extends ImmutableExecutionPoint {
}
