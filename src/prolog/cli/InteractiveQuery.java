// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.cli;

import prolog.exceptions.PrologError;
import prolog.execution.Environment;
import prolog.execution.ExecutionState;
import prolog.execution.Query;
import prolog.io.PrologReadInteractiveStream;
import prolog.io.PrologReadStream;
import prolog.io.PrologWriteStdoutStream;
import prolog.io.PrologWriteStream;
import prolog.io.Prompt;
import prolog.io.StructureWriter;
import prolog.io.WriteContext;
import prolog.variables.BoundVariable;

import java.io.IOException;
import java.util.Map;

/**
 * Execute an interactive query.
 */
public class InteractiveQuery extends Query {
    private static final PrologWriteStream OUT = PrologWriteStdoutStream.STREAM;
    private static final PrologReadStream IN = PrologReadInteractiveStream.STREAM;

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
                OUT.write("yes.\n");
                OUT.flush();
                return ExecutionState.SUCCESS;
            }
            boolean nl = false;
            WriteContext context = new WriteContext(environment, OUT);
            for (Map.Entry<String, BoundVariable> e : sortedVars.entrySet()) {
                if (nl) {
                    OUT.write("\n");
                }
                reportVar(context, e.getKey(), e.getValue());
                nl = true;
            }
            OUT.write(" ");
            OUT.flush();
            String text = readLine();
            if (text.equals(";")) {
                OUT.flush();
                return ExecutionState.BACKTRACK;
            } else {
                OUT.write("yes.\n");
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
    private String readLine() throws IOException {
        IN.setPrompt(Prompt.NONE);
        return IN.javaReader().readLine();
    }

    /**
     * Report a single variable
     *
     * @param context Write context
     * @param name    Name of variable
     * @param value   Value of variable
     * @throws IOException IO Exception if any
     */
    private void reportVar(WriteContext context, String name, BoundVariable value) throws IOException {
        OUT.write(" " + name + " <- ");
        new StructureWriter(context, value.value(environment)).write();
    }

    /**
     * Handle failure to report no solution found.
     */
    @Override
    protected void onFailed() {
        try {
            OUT.write("no.\n");
            OUT.flush();
        } catch (IOException ioe) {
            throw PrologError.systemError(environment, ioe);
        }
    }
}
