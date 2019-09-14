// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.test;

import java.nio.file.Path;

public interface Given {
    /**
     * Sets up precondition.
     *
     * @param text Prolog text, clause, fact, or query.
     * @return Self
     */
    Given that(String text);

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
     * Given specified working directory
     * @param directory New current working directory.
     */
    Given cwd(Path directory);
}
