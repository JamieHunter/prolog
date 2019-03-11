// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import prolog.constants.Atomic;
import prolog.execution.Environment;
import prolog.expressions.Term;

import java.io.BufferedReader;
import java.io.Closeable;

/**
 * Interface for Prolog read streams.
 */
public interface PrologReadStream extends Closeable {

    /**
     * @return Underlying Java reader
     */
    BufferedReader javaReader();

    /**
     * Read a single character as a constant
     *
     * @return character
     */
    Atomic getChar(Environment environment);

    /**
     * Read a term from input via parser
     *
     * @param environment Execution environment
     * @return parsed term
     */
    Term read(Environment environment);

    /**
     * For interactive streams, change the current prompt.
     *
     * @param prompt Prompt to show.
     */
    void setPrompt(Prompt prompt);
}
