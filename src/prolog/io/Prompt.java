// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

/**
 * Identifies prompts for interactive streams.
 */
public enum Prompt {
    /**
     * An interactive query is expected.
     */
    QUERY {
        @Override
        public String text() {
            return "?- ";
        }
    },
    /**
     * A term sentence is not complete (no '.')
     */
    CONTINUE {
        @Override
        public String text() {
            return "... ";
        }
    },
    /**
     * Interactive consult
     */
    CONSULT {
        @Override
        public String text() {
            return "| ";
        }
    },
    /**
     * No prompt
     */
    NONE;

    /**
     * @return prompt text, or null indicating no prompt.
     */
    public String text() {
        return null;
    }
}
