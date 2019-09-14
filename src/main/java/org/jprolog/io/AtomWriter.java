// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.io;

import org.jprolog.constants.PrologAtomLike;
import org.jprolog.expressions.Term;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to write an Atom in simplest form possible allowing it to be read correctly on re-parsing.
 */
public class AtomWriter extends TermWriter<PrologAtomLike> {

    private static final String GRAPHIC_TAG = "g";
    private static final String SOLO_TAG = "s";
    private static final String ALPHA_TAG = "a";

    private static final Pattern ACCEPT_PATTERN =
            Pattern.compile(or(
                    group(GRAPHIC_TAG, GRAPHIC),
                    group(SOLO_TAG, SOLO_GRAPHIC),
                    group(ALPHA_TAG, ALPHA_ATOM)), Pattern.DOTALL);

    public AtomWriter(WriteContext context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(Term t) throws IOException {
        PrologAtomLike term = (PrologAtomLike)t;
        Matcher m = ACCEPT_PATTERN.matcher(term.name());
        if (m.matches()) {
            if (m.group(GRAPHIC_TAG) != null) {
                context.beginGraphic();
            } else if (m.group(SOLO_TAG) != null) {
                context.beginSafe();
            } else {
                context.beginAlphaNum();
            }
            output.write(term.name());
        } else if (context.options().quoted) {
            writeQuoted('\'', term.name());
        } else if (term.name().length() == 0) {
            // write nothing
        } else {
            // guess if whitespace is required or not
            char first = term.name().charAt(0);
            char last = term.name().charAt(term.name().length()-1);
            if (first <= 0x20) {
                context.beginSafe();
            } else {
                context.beginUnknown();
            }
            output.write(term.name());
            if (last <= 0x20) {
                context.beginSafe();
            }
        }
    }

    /**
     * Return true if this atom must be quoted to be interpreted correctly
     * @param term Term to be tested
     * @return true if quoting is required
     */
    public static boolean needsQuoting(PrologAtomLike term) {
        Matcher m = ACCEPT_PATTERN.matcher(term.name());
        return !m.matches();
    }
}
