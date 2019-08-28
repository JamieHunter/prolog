// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.cli;

import prolog.bootstrap.DefaultIoBinding;
import prolog.exceptions.PrologError;
import prolog.exceptions.PrologThrowable;
import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.flags.ReadOptions;
import prolog.flags.WriteOptions;
import prolog.io.LogicalStream;
import prolog.io.Prompt;
import prolog.library.Io;

import java.io.IOException;

/**
 * CLI entrypoint.
 * TODO: No options processed yet.
 */
public class Run {

    /**
     * Main execution loop
     * @param args Options passed in
     */
    public static void main(String[] args) {
        Environment environment = new Environment();
        for (; ; ) {
            try {
                LogicalStream reader = DefaultIoBinding.USER_INPUT;
                try {
                    DefaultIoBinding.USER_OUTPUT.flush();
                    DefaultIoBinding.USER_ERROR.flush();
                } catch (IOException e) {
                    // ignore
                }
                reader.setPrompt(environment, null, Prompt.QUERY);
                Term term = reader.read(environment, null, new ReadOptions(environment, null));
                reader.setPrompt(environment, null, Prompt.NONE);
                if (term == Io.END_OF_FILE) {
                    return;
                }
                InteractiveQuery query = new InteractiveQuery(environment);
                query.compile(term);
                query.run();
            } catch (PrologThrowable pe) {
                displayError(environment, pe);
            } catch (RuntimeException re) {
                PrologThrowable pe = PrologError.convert(environment, re);
                displayError(environment, pe);
            }
        }
    }

    /**
     * Display a prolog error
     * @param environment Execution environment
     * @param pe Prolog Error
     */
    private static void displayError(Environment environment, PrologThrowable pe) {
        LogicalStream err = DefaultIoBinding.USER_ERROR;
        WriteOptions opt = new WriteOptions(environment, null);
        opt.quoted = true;
        err.write(environment, null, "Error while executing goal: ");
        err.write(environment, null, pe, opt);
        err.write(environment, null, "\n");
    }
}
