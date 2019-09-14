// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.variables;

import org.jprolog.enumerators.EnumTermStrategy;
import org.jprolog.expressions.Term;
import org.jprolog.instructions.DeferredCallInstruction;
import org.jprolog.instructions.ExecCall;
import org.jprolog.execution.CompileContext;
import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.io.WriteContext;

import java.io.IOException;

/**
 * Variable that exists, but has not been bound to a local context.
 */
public class UnboundVariable implements Variable {

    private final String name;
    private final long id;

    /**
     * Create with a name and unique ID
     *
     * @param name Name of variable as specified in text
     * @param id   Unique ID to disambiguate variables of same name
     */
    public UnboundVariable(String name, long id) {
        this.name = name;
        this.id = id;
    }

    /**
     * @return Name of variable
     */
    public String name() {
        return name;
    }

    /**
     * @return Unique ID of variable
     */
    public long id() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long corefId() {
        return id();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean instantiate(Term other) {
        throw new InternalError("Unexpected instantiation of unbound variable");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term value(Environment environment) {
        return this;
    }

    /**
     * Resolving a variable causes it to become bound.
     *
     * @param context binding context
     * @return Bound variable
     */
    @Override
    public Term resolve(LocalContext context) {
        return context.bind(this).resolve(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(WriteContext context) throws IOException {
        context.write(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInstantiated() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGrounded() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term enumTerm(EnumTermStrategy strategy) {
        return strategy.visitVariable(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term extract() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return name();
    }

    /**
     * Compiling a variable as a predicate creates an implicit call.
     *
     * @param compiling Context used for compilation.
     */
    @Override
    public void compile(CompileContext compiling) {
        // A compiled variable is considered the same as compiling 'call'
        compiling.addCall(this, new ExecCall(
                new DeferredCallInstruction(this)));
    }
}