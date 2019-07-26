// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import java.util.Optional;

/**
 * This manages a rich position, counting byte position, character position, line position, etc.
 */
public class Position implements Cloneable {
    private Long bytePos;
    private Long charPos;
    private Long linePos;
    private Long columnPos;

    /**
     * Capture byte position
     *
     * @param bytePos New position
     */
    public void setBytePos(long bytePos) {
        this.bytePos = bytePos;
    }

    /**
     * Capture character position
     *
     * @param charPos New position
     */
    public void setCharPos(long charPos) {
        this.charPos = charPos;
    }

    /**
     * Capture line position (0 before first line, 1 at first line, etc)
     *
     * @param linePos New position
     */
    public void setLinePos(long linePos) {
        this.linePos = linePos;
    }

    /**
     * Capture column position (0 before first line, 1 at first column of a line)
     *
     * @param columnPos New position
     */
    public void setColumnPos(long columnPos) {
        this.columnPos = columnPos;
    }

    /**
     * @return Byte position if known
     */
    public Optional<Long> getBytePos() {
        return Optional.ofNullable(bytePos);
    }

    /**
     * @return Character position if known
     */
    public Optional<Long> getCharPos() {
        return Optional.ofNullable(charPos);
    }

    /**
     * @return Line position if known
     */
    public Optional<Long> getLinePos() {
        return Optional.ofNullable(linePos);
    }

    /**
     * @return Column position if known
     */
    public Optional<Long> getColumnPos() {
        return Optional.ofNullable(columnPos);
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.getMessage(), e);
        }
    }

    /**
     * Reset structure allowing re-use.
     */
    public void reset() {
        bytePos = null;
        charPos = null;
        linePos = null;
        columnPos = null;
    }
}
