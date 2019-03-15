// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.cli;

import prolog.bootstrap.DefaultIoBinding;
import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.flags.ReadOptions;
import prolog.io.PrologReadInteractiveStream;
import prolog.io.PrologReadStream;
import prolog.io.Prompt;
import prolog.library.Io;

import java.io.IOException;

/**
 * CLI entrypoint.
 * TODO: No options processed yet.
 */
public class Run {
    public static void main(String [] args) {
        Environment environment = new Environment();
        for(;;) {
            PrologReadStream reader = PrologReadInteractiveStream.STREAM;
            try {
                DefaultIoBinding.USER_OUTPUT.getWrite().flush();
            } catch (IOException e) {
                // ignore
            }
            reader.setPrompt(Prompt.QUERY);
            Term term = reader.read(environment, new ReadOptions(environment, null));
            reader.setPrompt(Prompt.NONE);
            if (term == Io.END_OF_FILE) {
                return;
            }
            InteractiveQuery query = new InteractiveQuery(environment);
            query.compile(term);
            query.run();
        }
    }
}
