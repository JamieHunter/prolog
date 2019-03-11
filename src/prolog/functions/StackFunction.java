// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.functions;

import prolog.execution.CompileContext;

/**
 * A Mathematical function that can be compiled.
 */
public interface StackFunction {
    void compile(CompileContext compiling);
}
