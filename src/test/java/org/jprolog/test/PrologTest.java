// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.test;

import org.jprolog.test.internal.GivenImpl;

/**
 * All Prolog interpreter testing begins with this class.
 * Usually starting PrologTest.given()....
 */
public final class PrologTest {
    private PrologTest() {
        // Utility class
    }

    /**
     * Begin a test given/then/expect test chain.
     *
     * @return {@link Given}
     */
    public static Given given() {
        return new GivenImpl();
    }

    /**
     * Same as given().that(text).
     *
     * @return {@link Given}
     */
    public static Given given(String text) {
        return given().that(text);
    }
}
