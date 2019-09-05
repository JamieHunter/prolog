// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

/**
 * This is an IP that we can assume restores LocalContext and Scope. This allows
 * tail-call elimination.
 */
public interface RestoresLocalContext extends InstructionPointer {
}
