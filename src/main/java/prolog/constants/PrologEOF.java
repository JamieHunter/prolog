// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.constants;

import prolog.execution.EnumTermStrategy;
import prolog.execution.Environment;
import prolog.expressions.Container;
import prolog.expressions.Term;
import prolog.io.WriteContext;
import prolog.library.Io;

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
        return extract();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return extract().name();
    }

    /**
     * Retrieve the underlying atom
     *
     * @return atom
     */
    @Override
    public Term value(Environment environment) {
        return extract();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(WriteContext context) throws IOException {
        value(context.environment()).write(context);
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
    public PrologAtomInterned extract() {
        return Io.END_OF_FILE;
    }
}