// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.test;

import org.jprolog.execution.Environment;
import org.jprolog.expressions.Term;

import java.util.function.Consumer;

public interface Given {
    /**
     * Sets up precondition.
     *
     * @param text Prolog text, clause, fact, or query.
     * @return Self
     */
    Given that(String text);

    /**
     * Sets up precondition.
     *
     * @param term Prolog term, assumes clause or fact unless ?-(x) or :-(x).
     * @return Self
     */
    Given that(Term term);

    /**
     * Sets up precondition. Same as that(), but provides english sugar.
     *
     * @param text Prolog text, clause, fact, or query.
     * @return Self
     */
    Given and(String text);

    /**
     * Test portion of construct. Text is parsed (same as that()) but then goes into expectations.
     *
     * @param text Prolog text, clause, fact, or query.
     * @return {@link Then}
     */
    Then when(String text);

    /**
     * Assumes text is pre-parsed.
     *
     * @param term Prolog term, assumes clause or fact unless ?-(x) or :-(x).
     * @return {@link Then}
     */
    Then when(Term term);

    /**
     * For additional setup, obtains a then state, then calls lambda
     *
     * @param lambda Lambda function to further state
     * @return state also passed into lambda
     */
    Then when(Consumer<Then> lambda);

    /**
     * Perform environment operations
     *
     * @return environment Underlying environment
     */
    Given environment(Consumer<Environment> consumer);

    /**
     * @return Environment
     */
    Environment environment();

    /**
     * Creates a new context/environment.
     *
     * @return break' given
     */
    Given breakEnvironment();
}
