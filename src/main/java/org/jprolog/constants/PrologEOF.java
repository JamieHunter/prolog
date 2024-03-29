// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.constants;

import org.jprolog.enumerators.EnumTermStrategy;
import org.jprolog.expressions.Container;
import org.jprolog.expressions.Term;
import org.jprolog.io.WriteContext;
import org.jprolog.library.Io;

import java.io.IOException;

/**
 * Special EOF marker. Needed to differentiate EOF from token end_of_file during token parsing, so this effectively
 * puts the real EOF atom inside a container.
 */
public class PrologEOF extends AtomicBase implements Container {

    public static final PrologEOF EOF = new PrologEOF();

    private PrologEOF() {
    }

    /**
     * Retrieve the underlying atom
     *
     * @return atom
     */
    @Override
    public PrologAtomInterned get() {
        return value();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return value().name();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(WriteContext context) throws IOException {
        value().write(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term enumTerm(EnumTermStrategy strategy) {
        return strategy.visitContainer(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologAtomInterned value() {
        return Io.END_OF_FILE;
    }
}
