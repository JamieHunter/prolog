// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.exceptions;

import prolog.bootstrap.Interned;
import prolog.constants.Atomic;
import prolog.execution.Environment;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;

/**
 * Prolog Option/Flag error. An option error occurs when an option is invalid. Normally this is caught and converted to a
 * more specific domain error.
 */
public class FutureFlagValueError extends FutureFlagError {
    private final Atomic key;
    private final Term value;

    /**
     * Create a domain exception that is turned into a future PrologDomainError.
     *
     * @param key   Flag name
     * @param value Flag value
     */
    public FutureFlagValueError(Atomic key, Term value) {
        super(String.format("Domain option/flag value error: %s(%s)", key, value));
        this.key = key;
        this.value = value;
    }

    /**
     * @return Key(Value) as a compound term
     */
    @Override
    public Term getTerm() {
        return new CompoundTermImpl(key, value);
    }

    /**
     * @return Atomic name of flag
     */
    public Atomic getKey() {
        return key;
    }

    /**
     * @return Value that has the error
     */
    public Term getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologDomainError toError(Environment environment) {
        Term err = new CompoundTermImpl(Interned.PLUS_ATOM, key, value);
        return PrologDomainError.error(environment, environment.getAtom("flag_value"), err, this);
    }
}
