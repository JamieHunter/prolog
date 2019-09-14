// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.instructions;

import org.jprolog.constants.Atomic;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;

/**
 * Put an atomic value onto data stack
 */
public class ExecPushConstant implements Instruction {

    private final Atomic term;

    /**
     * Create instruction
     * @param term Atomic value to put onto stack
     */
    public ExecPushConstant(Atomic term) {
        this.term = term;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invoke(Environment environment) {
        environment.push(term);
    }
}
