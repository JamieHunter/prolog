// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.test.internal;

import prolog.test.Given;
import prolog.test.Then;

import java.nio.file.Path;

/**
 * PrologTest given clause. This sets up pre-conditions. For the interpreter, all preconditions are applied to
 * the environment.
 */
public class GivenImpl implements Given {

    private final StateImpl state = new StateImpl();

    @Override
    public Given that(String text) {
        when(text);
        return this;
    }

    @Override
    public Given and(String text) {
        return that(text);
    }

    @Override
    public Given cwd(Path directory) {
        state.environment().setCWD(directory);
        return this;
    }

    @Override
    public Then when(String text) {
        ThenImpl then = new ThenImpl(state);
        then.parse(text);
        return then;
    }

}
