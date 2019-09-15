// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.constants;

import org.jprolog.enumerators.EnumTermStrategy;
import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.Term;
import org.jprolog.bootstrap.Interned;

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
    public Term value() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term resolve(LocalContext context) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term enumTerm(EnumTermStrategy strategy) {
        return strategy.visitAtomic(this);
    }

    /**
     * Helper method to convert boolean flag to a boolean atom
     *
     * @param flag True or False
     * @return Atom "true" or "false"
     */
    protected static PrologAtomInterned atomize(boolean flag) {
        if (flag) {
            return Interned.TRUE_ATOM;
        } else {
            return Interned.FALSE_ATOM;
        }
    }
}
