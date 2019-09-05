// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.parser;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Line matcher for entire line. This is top-level matcher, and performs character translation.
 */
public class TopLineMatcher extends LineMatcher {
    private String source;
    private final Function<String,String> translator;
    private boolean hasSplit = false;

    public static String noTranslation(String source) {
        return source;
    }

    public TopLineMatcher(Pattern pattern, Function<String,String> translator) {
        super(pattern);
        this.translator = translator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void newLine(String text) {
        source = text;
        super.newLine(translator.apply(text));
    }

    /**
     * Create a child matcher (translation will be off for child).
     * @param newPattern Pattern for child matcher
     * @return child matcher
     */
    public SubLineMatcher split(Pattern newPattern) {
        assert !hasSplit;
        next();
        hasSplit = true;
        int pos = at();
        SubLineMatcher sub = new SubLineMatcher(newPattern, this);
        sub.setLine(source, pos);
        sub.setAt(pos);
        return sub;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (source == null) {
            return "";
        } else {
            return source;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void next() {
        assert !hasSplit;
        super.next();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void end() {
        hasSplit = false;
        this.next();
    }
}
