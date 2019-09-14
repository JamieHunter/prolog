// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.unification;

import org.jprolog.execution.LocalContext;

/**
 * Instruction of a unifier
 */
/*package*/ interface UnifyStep {
    /**
     * Execute this step
     *
     * @param context Local context for variable resolution.
     * @param it      Unify iterator
     * @return same or new iterator
     */
    UnifyIterator invoke(LocalContext context, UnifyIterator it);
}
