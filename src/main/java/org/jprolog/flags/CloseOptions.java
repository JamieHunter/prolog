// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.flags;

import org.jprolog.bootstrap.Interned;
import org.jprolog.exceptions.FutureFlagError;
import org.jprolog.exceptions.PrologDomainError;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.Term;

/**
 * Structured options parsed from a list of option atoms, used for closing streams.
 */
public class CloseOptions implements Flags {

    private static OptionParser<CloseOptions> parser = new OptionParser<>();

    static {
        // TODO: These are all placeholders and not yet parsed
        parser.booleanFlag(Interned.internAtom("force"), (o, v) -> o.force = v);
    }

    public boolean force = false;

    /**
     * Set this object of options from a list of option terms.
     *
     * @param environment Execution environment
     * @param optionsTerm List of options
     */
    public CloseOptions(Environment environment, Term optionsTerm) {
        try {
            parser.apply(environment, this, optionsTerm);
        } catch (FutureFlagError ffe) {
            throw PrologDomainError.error(environment, environment.internAtom("open_option"), ffe.getTerm(), ffe);
        }
    }
}
