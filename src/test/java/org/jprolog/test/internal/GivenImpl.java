// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.test.internal;

import org.jprolog.execution.Environment;
import org.jprolog.expressions.Term;
import org.jprolog.test.Given;
import org.jprolog.test.Then;

import java.util.function.Consumer;

/**
 * PrologTest given clause. This sets up pre-conditions. For the interpreter, all preconditions are applied to
 * the environment.
 */
public class GivenImpl implements Given {

    private final StateImpl state;

    public GivenImpl() {
        this.state = new StateImpl();
    }

    @Override
    public Given that(String text) {
        return when(text).and();
    }

    @Override
    public Given that(Term term) {
        return when(term).and();
    }

    @Override
    public Given and(String text) {
        return that(text);
    }

    @Override
    public Then when(String text) {
        ThenImpl then = new ThenImpl(this, state);
        then.run(text);
        return then;
    }

    @Override
    public Then when(Term term) {
        ThenImpl then = new ThenImpl(this, state);
        then.run(term);
        return then;
    }

    @Override
    public Then when(Consumer<Then> lambda) {
        ThenImpl then = new ThenImpl(this, state);
        lambda.accept(then);
        return then;
    }

    @Override
    public Given environment(Consumer<Environment> consumer) {
        consumer.accept(state.environment());
        return this;
    }

}
