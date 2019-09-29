// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.constants;

import org.jprolog.enumerators.EnumTermStrategy;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.Term;
import org.jprolog.expressions.TermList;
import org.jprolog.expressions.WorkingTermList;
import org.jprolog.io.StructureWriter;
import org.jprolog.io.WriteContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a list of code points or characters. This allows alternative representations of strings.
 * Representation is compact, and only split into less compact representation when it is being manipulated.
 * Note that it is assumed that the length of the sequence is at least one. The empty list ([]) is synonymous
 * with an empty list of codes or characters, therefore `` doesn't really exist.
 */
public abstract class PrologStringAsList implements TermList, WorkingTermList, Grounded {

    private final CharSequence value;

    public PrologStringAsList(CharSequence value) {
        assert value.length() != 0;
        this.value = value;
    }

    /**
     * Convert character to a term
     *
     * @param c Character to convert
     * @return converted character
     */
    protected abstract Term toTerm(char c);

    /**
     * Create a subsequence
     *
     * @param value Value of new subsequence (at least length 1)
     * @return subsequence as a list
     */
    protected abstract PrologStringAsList substring(CharSequence value);

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return value.toString();
    }

    /**
     * @return first character.
     */
    @Override
    public Term getHead() {
        return toTerm(value.charAt(0));
    }

    /**
     * @return list of characters excluding first character.
     */
    @Override
    public Term getTail() {
        if (value.length() > 1) {
            return substring(value.subSequence(1, value.length()));
        } else {
            return PrologEmptyList.EMPTY_LIST;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int concreteSize() {
        return value.length();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyMembers(ArrayList<Term> arr) {
        for (int i = 0; i < arr.size(); i++) {
            arr.add(toTerm(value.charAt(i)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TermList enumTerm(EnumTermStrategy strategy) {
        return strategy.visitStringAsList(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TermList enumAndCopyCompoundTermMembers(EnumTermStrategy strategy) {
        throw new UnsupportedOperationException("Use visitCodePoints");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TermList enumCompoundTermMembers(EnumTermStrategy strategy) {
        throw new UnsupportedOperationException("Use visitCodePoints");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TermList resolve(LocalContext context) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(WriteContext context) throws IOException {
        new StructureWriter(context).write(StructureWriter.NO_OPS, this);
    }

    /**
     * Ensure the natural string is returned
     *
     * @return String value of this string
     */
    public String getStringValue() {
        return value.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term getAt(int index) {
        if (index < 0 || index >= value.length()) {
            return null;
        } else {
            return toTerm(value.charAt(index));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkingTermList getTailList() {
        return (WorkingTermList) getTail();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkingTermList getFinalTailList() {
        return PrologEmptyList.EMPTY_LIST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmptyList() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConcrete() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Term> asList() {
        // TODO: more efficient version
        Term[] values = new Term[value.length()];
        for (int i = 0; i < values.length; i++) {
            values[i] = toTerm(value.charAt(i));
        }
        return Arrays.asList(values);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkingTermList subList(int n) {
        return substring(value.subSequence(n, value.length()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term toTerm() {
        return this;
    }
}
