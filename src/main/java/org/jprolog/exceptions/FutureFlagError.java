// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.exceptions;

import org.jprolog.expressions.Term;

/**
 * Prolog Option/Flag error. Base class for all option/flag errors
 */
public abstract class FutureFlagError extends FuturePrologError {

    protected FutureFlagError(String message) {
        super(message);
    }

    abstract public Term getTerm();
}
