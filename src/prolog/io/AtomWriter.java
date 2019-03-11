// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import prolog.constants.PrologAtom;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to write an Atom in simplest form possible allowing it to be read correctly on re-parsing.
 */
public class AtomWriter extends TermWriter<PrologAtom> {

    private static final String GRAPHIC_TAG = "g";
    private static final String SOLO_TAG = "s";
    private static final String ALPHA_TAG = "a";

    private static final Pattern ACCEPT_PATTERN =
            Pattern.compile(or(
                    group(GRAPHIC_TAG, GRAPHIC),
                    group(SOLO_TAG, SOLO_GRAPHIC),
                    group(ALPHA_TAG, ALPHA_ATOM)), Pattern.DOTALL);

    public AtomWriter(WriteContext context, PrologAtom prologAtom) {
        super(context, prologAtom);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write() throws IOException {
        Matcher m = ACCEPT_PATTERN.matcher(term.name());
        if (m.matches()) {
            if (m.group(GRAPHIC_TAG) != null) {
                context.beginGraphic();
            } else if (m.group(SOLO_TAG) != null) {
                context.beginSafe();
            } else {
                context.beginAlphaNum();
            }
            writer.write(term.name());
            return;
        }
        writeQuoted('\'', term.name());
    }
}
