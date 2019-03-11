// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.exceptions.PrologInstantiationError;
import prolog.exceptions.PrologTypeError;
import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.variables.Variable;

/**
 * Put a value that needs to be bound and resolved during runtime onto the stack. Value is required to be a number.
 */
public class ExecPushNumberVariable extends ExecPushVariable {

    /**
     * {@inheritDoc}
     */
    public ExecPushNumberVariable(Variable var) {
        super(var);
    }

    /**
     * Implement the check portion of {@link ExecPushVariable}.
     *
     * @param environment Execution environment
     * @param term        Term to check
     */
    @Override
    protected void check(Environment environment, Term term) {
        if (term.isInstantiated()) {
            if (!term.isNumber()) {
                throw PrologTypeError.numberExpected(environment, term);
            }
        } else {
            throw PrologInstantiationError.error(environment, term);
        }
    }
}
