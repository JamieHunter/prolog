package prolog.test.internal;


import prolog.execution.LocalContext;
import prolog.test.Given;
import prolog.test.Then;

import static org.junit.Assert.fail;

/**
 * PrologTest given clause. This sets up pre-conditions. For the interpreter, all preconditions are applied to
 * the environment.
 */
public class GivenImpl implements Given {

    protected final StateImpl state = new StateImpl();

    @Override
    public Given that(String text) {
        state.parse(text);
        return this;
    }

    @Override
    public Given and(String text) {
        state.parse(text);
        return this;
    }

    @Override
    public Then when(String text) {
        LocalContext context = state.parse(text);
        if (context == null) {
            fail("Directive did not return local localContext");
        }
        return new ThenImpl(state, context);
    }

}
