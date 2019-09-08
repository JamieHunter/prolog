// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.test;

import org.hamcrest.Matcher;
import prolog.expressions.Term;

import java.util.function.Consumer;

public interface Then {
    /**
     * Retrieve the value of a given variable name
     *
     * @param name    Name of variable.
     * @return value of named variable
     */
    Term getVariableValue(String name);

    /**
     * Verify a variable is as expected. What mostly meaningful for successful queries, it also has meaning
     * for some failure cases.
     *
     * @param name    Name of variable.
     * @param matcher Match variable content
     * @return self
     */
    Then variable(String name, Matcher<? super Term> matcher);

    /**
     * Verify one or more log entries. Note that expectLog() or expectLog(null) verifies end of log status.
     * expectLog() may be specified multiple times, each time consumes more of the log.
     *
     * @param matchers Multiple matches to match variable content, null indicates end of content.
     * @return self
     */
    @SuppressWarnings("unchecked")
    Then expectLog(Matcher<? super Term>... matchers);

    /**
     * Verify query was successful
     *
     * @return self
     */
    Then assertSuccess();

    /**
     * Verify query failed
     *
     * @return self
     */
    Then assertFailed();

    /**
     * Verify call depth (for tail-call elimination tests)
     *
     * @param matcher Matcher for integer value
     * @return self
     */
    Then callDepth(Matcher<? super Integer> matcher);

    /**
     * Verify backtrack depth (for tail-call elimination tests)
     *
     * @param matcher Matcher for integer value
     * @return self
     */
    Then backtrackDepth(Matcher<? super Integer> matcher);

    /**
     * Begins anotherSolution test
     *
     * @param text Same as when(text)
     * @return self
     */
    Then andWhen(String text);

    /**
     * For additional setup
     * @param lambda Lambda function to further state
     * @return state also passed into lambda
     */
    Then andWhen(Consumer<Then> lambda);

    /**
     * Another solution, same test.
     * @return self
     */
    Then anotherSolution();
}
