// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.expressions;

import prolog.expressions.Term;

/**
 * A container term is a term that requires value() to be called to obtain a value that can be inspected or unified.
 * In particular, variables are containers.
 */
public interface Container extends Term {
}
