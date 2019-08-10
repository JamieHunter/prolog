// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.parser;

/**
 * Perform/query character conversion (per char_conversion spec)
 */
public class CharConverter {
    private boolean enabled;
    private char [] table = new char[0];

    /**
     * Enable conversion. Conversion is not enabled until this point.
     * @param f True if enabled
     */
    public void enableConversion(Boolean f) {
        enabled = f;
    }

    /**
     * @return Enabled state
     */
    public boolean isEnabled() {
        return enabled;
    }

    public int getSize() {
        return table.length;
    }

    /**
     * Retrieve conversion for specified character
     * @param source Character to convert
     * @return conversion
     */
    public char convert(char source) {
        if (source >= table.length) {
            return source;
        } else {
            return table[source];
        }
    }

    /**
     * Called prior to iteration
     */
    public void initialize() {
        grow(0);
    }

    /**
     * Grow table size as needed
     * @param size Minimum new size
     */
    protected void grow(int size) {
        if (size < 128) {
            size = 128;
        }
        if (size < table.length) {
            return;
        }
        char [] newTable = new char[size];
        for(int i = 0; i < table.length; i++) {
            newTable[i] = table[i];
        }
        for(int i = table.length; i < size; i++) {
            newTable[i] = (char)i;
        }
        table = newTable;
    }

    /**
     * Add a new conversion
     * @param source Source character to convert
     * @param target Target after conversion
     */
    public void add(char source, char target) {
        if (source >= table.length) {
            if (source == target) {
                return; // no-op
            }
            grow(source+1);
        }
        table[source] = target;
    }

    /**
     * Scans table for desired character
     *
     * @param start start of scan
     * @param limit limit of scan
     * @param test character to scan for
     * @return index if found, or limit if not found
     */
    public int scan(int start, int limit, char test) {
        int limitAdj = Math.min(table.length, limit);
        for(int i = start; i < limitAdj; i++) {
            if (table[i] == test) {
                return i;
            }
        }
        return limit;
    }
}
