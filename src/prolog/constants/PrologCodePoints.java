// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.constants;

import prolog.execution.EnumTermStrategy;
import prolog.execution.LocalContext;
import prolog.expressions.CompoundTerm;
import prolog.expressions.TermList;
import prolog.expressions.Term;
import prolog.io.TermWriter;
import prolog.io.WriteContext;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a list of code points. This is `string` vs "string". Representation is compact,
 * and only split into less compact representation when it is being manipulated. Note that it
 * is assumed that the length of the sequence is at least one. The empty list ([]) is synonymous
 * with an empty list of code points, therefore `` doesn't really exist.
 */
public class PrologCodePoints implements TermList, Grounded {

    private final CharSequence value;

    public PrologCodePoints(CharSequence value) {
        this.value = value;
    }

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
        return new PrologCharacter(value.charAt(0));
    }

    /**
     * @return list of characters excluding first character.
     */
    @Override
    public Term getTail() {
        if (value.length() > 1) {
            return new PrologCodePoints(value.subSequence(1, value.length()));
        } else {
            return PrologEmptyList.EMPTY_LIST;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int length() {
        return value.length();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyMembers(ArrayList<Term> arr) {
        for (int i = 0; i < arr.size(); i++) {
            arr.add(new PrologCharacter(value.charAt(i)));
        }
    }

    @Override
    public TermList enumTerm(EnumTermStrategy strategy) {
        return strategy.visitCodePoints(this);
    }

    @Override
    public TermList mutateCompoundTerm(EnumTermStrategy strategy) {
        throw new UnsupportedOperationException("Use visitCodePoints");
    }

    @Override
    public TermList enumCompoundTerm(EnumTermStrategy strategy) {
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
        new TermWriter<PrologCodePoints>(context, this) {
            @Override
            public void write() throws IOException {
                writeQuoted('`', value.toString());
            }
        }.write();
    }
}
