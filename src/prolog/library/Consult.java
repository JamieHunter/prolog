// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.DemandLoad;
import prolog.bootstrap.Predicate;
import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.io.PrologReadStream;
import prolog.io.Prompt;
import prolog.predicates.Predication;

import static prolog.bootstrap.Builtins.predicate;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Bootstraps consult. Consult is predominantly implemented as a Prolog script that needs to be bootstrapped.
 */
public final class Consult {
    private Consult() {
        // Static methods/fields only
    }

    /**
     * Primes interactive prompt to indicate interactive consult.
     *
     * @param environment Execution environment
     * @param streamName  Single term specifying stream.
     */
    @Predicate("$consult_prompt")
    public static void consultPrompt(Environment environment, Term streamName) {
        PrologReadStream stream = Io.getReader(environment, streamName, null);
        stream.setPrompt(Prompt.CONSULT);
    }

    /**
     * Ends interactive prompt.
     *
     * @param environment Execution environment
     * @param streamName  Single term specifying stream.
     */
    @Predicate("$no_prompt")
    public static void noPrompt(Environment environment, Term streamName) {
        PrologReadStream stream = Io.getReader(environment, streamName, null);
        stream.setPrompt(Prompt.NONE);
    }

    /**
     * List of predicates defined by the resource "consult.pl".
     */
    @DemandLoad("consult.pl")
    public static Predication consult[] = {
            predicate("consult", 1),
            predicate(".", 2),
            predicate("load_files", 2)
    };
}
