// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.parser;

import java.util.regex.Pattern;

/**
 * Line matcher for part of the line, no translation performed. A different regex is used. No translation is
 * performed.
 */
public class SubLineMatcher extends LineMatcher {

    private final LineMatcher parent;

    public SubLineMatcher(Pattern pattern, LineMatcher parent) {
        super(pattern);
        this.parent = parent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void newLine(String text) {
        parent.newLine(text); // make sure parent receives new line too
        super.newLine(text);
    }

    /**
     * {@inheritDoc}
     */
    public void setLine(String source, int pos) {
        super.newLine(source);
        super.setAt(pos);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAtEnd() {
        super.setAtEnd();
        parent.setAtEnd();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void next() {
        super.next();
        parent.setAt(at()); // let parent track position
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LineMatcher split(Pattern pattern) {
        next();
        return new SubLineMatcher(pattern, this);
    }

    /**
     * End sub-line matching, make sure parent is synchronized.
     */
    @Override
    public void end() {
        super.end();
        parent.setAt(at());
        parent.end();
    }
}
