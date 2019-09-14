// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.functions;

import org.jprolog.execution.Instruction;

import java.util.List;

/**
 * A Mathematical function that can be compiled.
 */
public interface StackFunction {
    void compile(List<Instruction> compiling);
}
