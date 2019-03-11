package prolog.test.internal;


import org.hamcrest.Matcher;
import prolog.execution.LocalContext;
import prolog.expressions.Term;
import prolog.test.Then;
import prolog.variables.BoundVariable;
import prolog.variables.UnboundVariable;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * PrologTest then clause. This clause allows testing results of a directive.
 */
public class ThenImpl implements Then {

    protected final StateImpl state;
    protected final LocalContext context;

    public ThenImpl(StateImpl state, LocalContext context) {
        this.state = state;
        this.context = context;
    }

    @Override
    public Then variable(String name, Matcher<? super Term> matcher) {
        BoundVariable var = state.vars.get(name);
        if (var == null) {
            var = new BoundVariable(context, name, 0);
        }
        Term resolved = var.resolve(state.environment.getLocalContext());
        resolved = resolved.value(state.environment);
        match(matcher, resolved);
        return this;
    }

    @Override
    public Then assertSuccess() {
        assertTrue("directive failed", state.succeeded);
        return this;
    }

    @Override
    public Then assertFailed() {
        assertTrue("directive succeeded", !state.succeeded);
        return this;
    }

    @Override
    public Then callDepth(Matcher<? super Integer> m) {
        assertThat(state.callDepth, m);
        return this;
    }

    @Override
    public Then backtrackDepth(Matcher<? super Integer> m) {
        assertThat(state.backtrackDepth, m);
        return this;
    }

    @Override
    public Then andWhen(String text) {
        LocalContext context = state.parse(text);
        if (context == null) {
            fail("Directive did not return local localContext");
        }
        return new ThenImpl(state, context);
    }

    private void match(Matcher<? super Term> matcher, Term value) {
        assertThat(value, matcher);
    }
}
