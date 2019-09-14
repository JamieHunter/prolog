// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.cli;

import org.jprolog.bootstrap.DefaultIoBinding;
import org.jprolog.exceptions.PrologError;
import org.jprolog.execution.Environment;
import org.jprolog.execution.ExecutionState;
import org.jprolog.execution.Query;
import org.jprolog.flags.WriteOptions;
import org.jprolog.variables.BoundVariable;
import org.jprolog.io.LogicalStream;
import org.jprolog.io.Prompt;

import java.io.IOException;
import java.util.Map;

/**
 * Execute an interactive query.
 */
public class InteractiveQuery extends Query {
    private static final LogicalStream OUT = DefaultIoBinding.USER_OUTPUT;
    private static final LogicalStream IN = DefaultIoBinding.USER_INPUT;

    InteractiveQuery(Environment environment) {
        super(environment);
    }

    /**
     * Handle onSuccess to report all variables and allow interactive search for more solutions.
     *
     * @return {@link ExecutionState#BACKTRACK} to force search for more solutions.
     */
    @Override
    protected ExecutionState onSuccess() {
        try {
            Map<String, BoundVariable> sortedVars = context.retrieveVariableMap();
            if (sortedVars.isEmpty()) {
                OUT.write(environment, null, "yes.\n");
                OUT.flush();
                return ExecutionState.SUCCESS;
            }
            boolean nl = false;
            for (Map.Entry<String, BoundVariable> e : sortedVars.entrySet()) {
                if (nl) {
                    OUT.write(environment, null, "\n");
                }
                reportVar(e.getKey(), e.getValue());
                nl = true;
            }
            OUT.write(environment, null, " ");
            OUT.flush();
            String text = readLine();
            if (text.equals(";")) {
                OUT.flush();
                return ExecutionState.BACKTRACK;
            } else {
                OUT.write(environment, null, "yes.\n");
                OUT.flush();
                return ExecutionState.SUCCESS;
            }
        } catch (IOException ioe) {
            throw PrologError.systemError(environment, ioe);
        }
    }

    /**
     * Read line of input, with no prompt.
     *
     * @return Line of input
     * @throws IOException on IO error
     */
    private String readLine() {
        IN.setPrompt(environment, null, Prompt.NONE);
        return IN.readLine(environment, null, null);
    }

    /**
     * Report a single variable
     *
     * @param name  Name of variable
     * @param value Value of variable
     * @throws IOException IO Exception if any
     */
    private void reportVar(String name, BoundVariable value) throws IOException {
        OUT.write(environment, null, " " + name + " <- ");
        WriteOptions options = new WriteOptions(environment, null);
        options.quoted = true;
        options.numbervars = true;
        OUT.write(environment, null, value.value(environment), options);
    }

    /**
     * Handle failure to report no solution found.
     */
    @Override
    protected void onFailed() {
        try {
            OUT.write(environment, null, "no.\n");
            OUT.flush();
        } catch (IOException ioe) {
            throw PrologError.systemError(environment, ioe);
        }
    }
}
