// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class, manage reg-ex matching over an entire line of input
 */
public abstract class LineMatcher {
    private final Pattern pattern;
    private Matcher matcher;
    private boolean matched = false;

    public LineMatcher(Pattern pattern) {
        this.pattern = pattern;
    }

    /**
     * Begin a new line
     *
     * @param text Text for line
     */
    public void newLine(String text) {
        matcher = pattern.matcher(text);
        setAt(0);
    }

    /**
     * Move to specific position
     *
     * @param offset New position
     */
    public void setAt(int offset) {
        matched = false;
        matcher.region(offset, matcher.regionEnd());
    }

    /**
     * Move to end
     */
    public void setAtEnd() {
        if (matcher != null) {
            setAt(matcher.regionEnd());
        }
    }

    /**
     * Current match region start
     *
     * @return region start
     */
    public int at() {
        if (matcher == null) {
            return 0;
        } else {
            return matcher.regionStart();
        }
    }

    /**
     * @return true if at end of region
     */
    public boolean atEnd() {
        return matcher == null || at() == matcher.regionEnd();
    }

    /**
     * Set start of region to be end of last match, error if already moved
     */
    public void next() {
        if (matched) {
            matched = false;
            matcher.region(matcher.end(), matcher.regionEnd());
        }
    }

    /**
     * Call to finish with sub-match, synchronize top matcher with sub-matcher
     */
    public void end() {
        next();
    }

    /**
     * compare pattern with next block of text.
     *
     * @return true if match succeeded
     */
    public boolean scanNext() {
        next();
        if (matcher.lookingAt()) {
            matched = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Find next pattern that matches.
     *
     * @return index of match, -1 if no match.
     */
    public int find() {
        next();
        if (matcher.find()) {
            matched = true;
            return matcher.start();
        } else {
            return -1;
        }
    }

    /**
     * Return sub-group by name.
     *
     * @param name Name of sub-group
     * @return text of match, or null if no match
     */
    public String group(String name) {
        return matcher.group(name);
    }

    /**
     * @return entire match group
     */
    public String group() {
        return matcher.group();
    }

    /**
     * @return entire line
     */
    @Override
    public String toString() {
        if (matcher == null) {
            return "";
        } else {
            return matcher.toString();
        }
    }

    /**
     * Create a sub-match with a different pattern
     *
     * @param pattern New pattern
     * @return sub-match
     */
    public abstract LineMatcher split(Pattern pattern);
}
