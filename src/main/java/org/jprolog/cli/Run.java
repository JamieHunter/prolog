// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.cli;

import org.jprolog.bootstrap.DefaultIoBinding;
import org.jprolog.bootstrap.Interned;
import org.jprolog.exceptions.PrologAborted;
import org.jprolog.exceptions.PrologError;
import org.jprolog.exceptions.PrologHalt;
import org.jprolog.exceptions.PrologThrowable;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.Term;
import org.jprolog.flags.ReadOptions;
import org.jprolog.flags.WriteOptions;
import org.jprolog.library.Io;
import org.jprolog.io.LogicalStream;
import org.jprolog.io.Prompt;

import java.io.IOException;

/**
 * CLI entrypoint.
 * TODO: No options processed yet.
 */
public class Run {
    private final Environment environment;

    public Run(Environment environment) {
        this.environment = environment;
    }

    /**
     * Main execution loop
     * @param args Options passed in
     */
    public static void main(String[] args) {
        Environment environment = new Environment();
        new Run(environment).run();
    }

    public void run() {
        for (; ; ) {
            try {
                LogicalStream reader = DefaultIoBinding.USER_INPUT;
                if (environment.getBreakLevel() > 0) {
                    DefaultIoBinding.USER_OUTPUT.write(environment, null, String.format("[%d]", environment.getBreakLevel()));
                }
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
                if (environment.getBreakLevel() > 0 && term.compareTo(Interned.ABORT_ATOM) == 0) {
                    return;
                }
                InteractiveQuery query = new InteractiveQuery(environment);
                query.compile(term);
                query.run();
            } catch (PrologHalt ph) {
                System.exit(ph.getHaltCode());
            } catch (PrologAborted pa) {
                displaySimpleError(pa);
                environment.abortReset();
            } catch (PrologThrowable pe) {
                displayError(pe);
            } catch (RuntimeException re) {
                PrologThrowable pe = PrologError.convert(environment, re);
                displayError(pe);
            }
        }
    }

    /**
     * Display a prolog error
     * @param pe Prolog Error
     */
    private void displayError(PrologThrowable pe) {
        LogicalStream err = DefaultIoBinding.USER_ERROR;
        WriteOptions opt = new WriteOptions(environment, null);
        opt.quoted = true;
        err.write(environment, null, "Error while executing goal: ");
        err.write(environment, null, pe, opt);
        err.write(environment, null, "\n");
    }

    /**
     * Display a simple error message
     * @param pe Prolog Error
     */
    private void displaySimpleError(PrologThrowable pe) {
        String msg = pe.getMessage();
        if (msg == null || msg.length() == 0) {
            displayError(pe);
            return;
        }
        LogicalStream err = DefaultIoBinding.USER_ERROR;
        err.write(environment, null, pe.getMessage());
        err.write(environment, null, "\n");
    }
}
