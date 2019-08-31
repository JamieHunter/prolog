// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.parser;

import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.flags.ReadOptions;
import prolog.io.InputBuffered;
import prolog.io.InputDecoderFilter;
import prolog.io.SequentialInputStream;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class to parse a string.
 */
public class StringParser {

    private StringParser() {
        // Utility
    }

    /**
     * Utility to parse a string as a term.
     *
     * @param environment Execution environment
     * @param text        Text - from atom or string
     * @param options     Options to apply
     * @return Parsed term
     */
    public static Term parse(Environment environment, String text, ReadOptions options) {
        InputBuffered stream = new InputBuffered(
                new InputDecoderFilter(
                        new SequentialInputStream(
                                new ByteArrayInputStream(
                                        text.getBytes()
                                )),
                        StandardCharsets.UTF_8),
                -1);
        Tokenizer tok = new Tokenizer(environment, options, stream);
        ExpressionReader reader = new ExpressionReader(tok);
        return reader.read();
    }

}
