// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.test;

import org.hamcrest.Matcher;
import org.jprolog.expressions.Term;
import org.jprolog.test.internal.OutputMonitor;

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
     * @return true if query was successful.
     */
    boolean isSuccess();

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
     * More generalized form of and-when etc.
     * @return new given, same state
     */
    Given and();

    /**
     * Begins anotherSolution test
     *
     * @param text Same as when(text)
     * @return self
     */
    Then andWhen(String text);

    /**
     * Another solution, same test.
     * @return self
     */
    Then anotherSolution();

    /**
     * Iterate all solutions.
     * @return self
     */
    Then solutions(Consumer<Then>... solutions);

    /**
     * Retrieve a '##text_log' textual monitor
     * @param alias Alias as when text_log was created
     * @return monitor
     */
    OutputMonitor getTextLog(String alias);

    /**
     * Test the given alias match count is as given.
     * @param alias Alias to monitor
     * @param matcher Matcher to validate value
     * @return self
     */
    Then textMatches(String alias, Matcher<Integer> matcher);
}
