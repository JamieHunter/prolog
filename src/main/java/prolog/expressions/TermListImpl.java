// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.expressions;

import prolog.constants.PrologEmptyList;
import prolog.execution.CompileContext;
import prolog.execution.EnumTermStrategy;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.io.StructureWriter;
import prolog.io.WriteContext;
import prolog.predicates.PredicateDefinition;
import prolog.predicates.Predication;
import prolog.unification.HeadTailUnifyIterator;
import prolog.unification.UnifyIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Compact form of a list. That is, an expression of form [A,B,C,D|Tail] is expressed as a TermList, and behaves
 * as a tree of '.'(A, '.'(B, '.'(C, '.'(D, Tail))))
 */
public class TermListImpl implements TermList {

    protected int index;
    protected final Term[] terms;
    protected final Term tail;

    /**
     * Construct a TermList from an array of terms, and a final tail.
     *
     * @param terms Terms of the list
     * @param tail  Final tail, e.g. Tail of [A,B|Tail], or [] if no tail.
     */
    public TermListImpl(Term[] terms, Term tail) {
        this.terms = terms;
        this.tail = tail;
    }

    /**
     * Construct a TermList from a Java list of terms, and a final tail.
     *
     * @param terms Terms of the list
     * @param tail  Final tail, e.g. Tail of [A,B|Tail], or [] if no tail.
     */
    public TermListImpl(List<Term> terms, Term tail) {
        this(terms.toArray(new Term[terms.size()]), tail);
    }

    /**
     * Construct a truncated TermList from an array of terms, and a final tail.
     *
     * @param index Truncation index (skip index items)
     * @param terms Terms of the list (including those skipped)
     * @param tail  Final tail, e.g. Tail of [A,B|Tail], or [] if no tail.
     */
    public TermListImpl(int index, Term[] terms, Term tail) {
        this.index = index;
        this.terms = terms;
        this.tail = tail;
    }

    /**
     * Attempt to make list into a grounded list
     *
     * @param context local contact (used e.g. to create a bound variable)
     * @return resolved list
     */
    @Override
    public TermList resolve(LocalContext context) {
        // Algorithm copies on change
        Term[] target;
        boolean changed;
        if (index > 0) {
            // always copy if index > 0
            target = Arrays.copyOfRange(terms, index, terms.length);
            changed = true;
        } else {
            target = terms;
            changed = false;
        }
        boolean grounded = true;
        for (int i = 0; i < target.length; i++) {
            Term orig = target[i];
            Term res = orig.resolve(context);
            if (orig != res) {
                if (!changed) {
                    target = target.clone();
                    changed = true;
                }
                target[i] = res;
            }
            grounded = grounded && res.isGrounded();
        }
        Term newTail = tail.resolve(context);
        if (newTail != tail) {
            changed = true;
        }
        grounded = grounded && newTail.isGrounded();
        if (grounded) {
            return new GroundedTermList(0, target, newTail);
        } else if (!changed) {
            return this;
        } else {
            return new TermListImpl(0, target, newTail);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TermList enumTerm(EnumTermStrategy strategy) {
        return (TermList) strategy.visitCompoundTerm(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TermList enumAndCopyCompoundTermMembers(EnumTermStrategy strategy) {
        return (TermList) strategy.computeUncachedTerm(this, tt -> {
            Term[] copy = Arrays.copyOfRange(terms, index, terms.length);
            boolean grounded = true;
            for (int i = 0; i < copy.length; i++) {
                Term t = copy[i].enumTerm(strategy);
                grounded = grounded && t.isGrounded();
                copy[i] = t;
            }
            Term newTail = tail.enumTerm(strategy);
            grounded = grounded && newTail.isGrounded();
            if (grounded) {
                return new GroundedTermList(0, copy, newTail);
            } else {
                return new TermListImpl(0, copy, newTail);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TermList enumCompoundTermMembers(EnumTermStrategy strategy) {
        for (int i = index; i < terms.length; i++) {
            terms[i].enumTerm(strategy);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(WriteContext context) throws IOException {
        new StructureWriter(context).write(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnifyIterator getUnifyIterator() {
        return new HeadTailUnifyIterator(this);
    }

    /**
     * Compile this compound term. This will use the .() definition
     *
     * @param compiling Context
     */
    @Override
    public void compile(CompileContext compiling) {
        Environment.Shared environmentShared = compiling.environmentShared();
        Predication predication = new Predication(functor(), arity());
        PredicateDefinition definition = environmentShared.autoCreateDictionaryEntry(predication);
        definition.compile(predication, compiling, this);
    }

    /**
     * newList is used by Tail to create a new child list. It is implemented to create a list of the same type
     * as the implementing class.
     *
     * @param index Subscript of list (skip index items)
     * @param terms Full array of terms
     * @param tail  Tail term (rest of list)
     * @return New sublist
     */
    protected TermListImpl newList(int index, Term[] terms, Term tail) {
        return new TermListImpl(index, terms, tail);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term getHead() {
        if (index == terms.length) {
            return tail;
        } else {
            return terms[index];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term getTail() {
        if (index + 1 >= terms.length) {
            return tail;
        } else {
            return newList(index + 1, terms, tail);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int length() {
        return terms.length - index;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyMembers(ArrayList<Term> arr) {
        arr.addAll(Arrays.asList(terms));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term lastTail() {
        return tail;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        builder.append(getHead().toString());
        for (int i = index + 1; i < terms.length; i++) {
            builder.append(',');
            builder.append(terms[i].toString());
        }
        if (tail != PrologEmptyList.EMPTY_LIST) {
            builder.append('|');
            builder.append(tail.toString());
        }
        builder.append(']');
        return builder.toString();
    }
}
