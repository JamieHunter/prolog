// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.constants;

import prolog.bootstrap.Interned;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.expressions.Term;

/**
 * A base class for atomic constants, providing common functionality.
 */
public abstract class AtomicBase implements Atomic {

    /**
     * {@inheritDoc}
     */
    public boolean isGrounded() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAtomic() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term value(Environment environment) {
        return this;
    }

    /**
     * Constants don't need to be bound as they are already bound.
     *
     * @param context not used
     * @return self.
     */
    @Override
    public Term resolve(LocalContext context) {
        return this;
    }

    /**
     * Helper method to convert boolean flag to a boolean atom
     *
     * @param flag True or False
     * @return Atom "true" or "false"
     */
    protected static PrologAtom atomize(boolean flag) {
        if (flag) {
            return Interned.TRUE_ATOM;
        } else {
            return Interned.FALSE_ATOM;
        }
    }
}
