// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.exceptions;

import prolog.constants.Atomic;
import prolog.execution.Environment;
import prolog.expressions.Term;

/**
 * Prolog Option/Flag error. Option/flag cannot be written
 */
public class FutureFlagPermissionError extends FutureFlagError {
    private final Atomic key;

    /**
     * Create a permission exception that is turned into a future PrologPermissionError.
     *
     * @param key   Name of flag
     */
    public FutureFlagPermissionError(Atomic key) {
        super("Cannot modify option/flag: " + key.toString());
        this.key = key;
    }

    /**
     * @return Key as the term
     */
    @Override
    public Term getTerm() {
        return key;
    }

    /**
     * @return Flag that has the error
     */
    public Atomic getKey() {
        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologPermissionError toError(Environment environment) {
        return PrologPermissionError.error(environment,
                environment.internAtom("modify"),
                environment.internAtom("flag"),
                key,
                getMessage(),
                this);
    }
}
