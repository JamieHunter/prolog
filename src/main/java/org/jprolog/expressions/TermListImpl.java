// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.expressions;

import org.jprolog.constants.PrologEmptyList;
import org.jprolog.enumerators.EnumTermStrategy;
import org.jprolog.execution.CompileContext;
import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.io.StructureWriter;
import org.jprolog.io.WriteContext;
import org.jprolog.predicates.PredicateDefinition;
import org.jprolog.predicates.Predication;
import org.jprolog.unification.HeadTailUnifyIterator;
import org.jprolog.unification.UnifyIterator;
import org.jprolog.utility.SubList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Compact form of a list. That is, an expression of form [A,B,C,D|Tail] is expressed as a TermList, and behaves
 * as a tree of '.'(A, '.'(B, '.'(C, '.'(D, Tail))))
 */
public class TermListImpl implements TermList, WorkingTermList {

    protected final SubList<Term> terms;
    protected final Term tail;

    /**
     * Construct a TermList from a list of terms, and a final tail.
     *
     * @param terms Terms of the list
     * @param tail  Final tail, e.g. Tail of [A,B|Tail], or [] if no tail.
     */
    public TermListImpl(List<? extends Term> terms, Term tail) {
        this.terms = SubList.wrap(terms);
        this.tail = tail;
    }

    /**
     * Attempt to make list into a grounded list
     *
     * @param context local contact (used e.g. to activate labeled variables)
     * @return resolved list
     */
    @Override
    public TermList resolve(LocalContext context) {
        // Algorithm copies on change
        List<Term> target = null;
        boolean changed = false;
        boolean grounded = true;
        for (int i = 0; i < terms.size(); i++) {
            Term orig = terms.get(i);
            Term res = orig.resolve(context);
            if (orig != res) {
                if (!changed) {
                    target = new ArrayList<>(terms);
                    changed = true;
                }
                target.set(i, res);
            }
            grounded = grounded && res.isGrounded();
        }
        Term newTail = tail.resolve(context);
        if (newTail != tail) {
            changed = true;
        }
        if (target == null) {
            target = terms;
        }
        grounded = grounded && newTail.isGrounded();
        if (grounded) {
            return new GroundedTermList(target, newTail);
        } else if (!changed) {
            return this;
        } else {
            return new TermListImpl(target, newTail);
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
            List<Term> copy = new ArrayList<>(terms);
            boolean grounded = true;
            for (int i = 0; i < copy.size(); i++) {
                Term t = copy.get(i).enumTerm(strategy);
                grounded = grounded && t.isGrounded();
                copy.set(i, t);
            }
            Term newTail = tail.enumTerm(strategy);
            grounded = grounded && newTail.isGrounded();
            if (grounded) {
                return new GroundedTermList(copy, newTail);
            } else {
                return new TermListImpl(copy, newTail);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TermList enumCompoundTermMembers(EnumTermStrategy strategy) {
        for (int i = 0; i < terms.size(); i++) {
            terms.get(i).enumTerm(strategy);
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
     * @param terms Full array of terms
     * @param tail  Tail term (rest of list)
     * @return New sublist
     */
    protected TermListImpl newList(List<Term> terms, Term tail) {
        return new TermListImpl(terms, tail);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term getHead() {
        if (terms.size() == 0) {
            return tail;
        } else {
            return terms.get(0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term getTail() {
        if (terms.size() <= 1) {
            return tail;
        } else {
            return newList(terms.subList(1), tail);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkingTermList getFinalTailList() {
        if (tail instanceof WorkingTermList) {
            return (WorkingTermList)tail;
        } else {
            return new WorkingTermListTail(tail);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkingTermList getTailList() {
        Term tail = getTail();
        if (tail instanceof WorkingTermList) {
            return (WorkingTermList)tail;
        } else {
            return new WorkingTermListTail(tail);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term getAt(int i) {
        int limit = concreteSize();
        if (i < 0 || i >= limit) {
            return null;
        } else {
            return terms.get(i);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConcrete() {
        return tail == PrologEmptyList.EMPTY_LIST;
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
    public int concreteSize() {
        return terms.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Term> asList() {
        return terms;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyMembers(ArrayList<Term> arr) {
        arr.addAll(terms);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term lastTail() {
        return tail;
    }

    @Override
    public WorkingTermList subList(int n) {
        if (n < 0 || n > terms.size()) {
            throw new IllegalArgumentException("Specified an index out of range");
        }
        if (n == terms.size()) {
            return getFinalTailList();
        } else {
            return new TermListImpl(terms.subList(n), tail);
        }
    }

    @Override
    public Term toTerm() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        builder.append(getHead().toString());
        for (int i = 1; i < terms.size(); i++) {
            builder.append(',');
            builder.append(terms.get(i).toString());
        }
        if (tail != PrologEmptyList.EMPTY_LIST) {
            builder.append('|');
            builder.append(tail.toString());
        }
        builder.append(']');
        return builder.toString();
    }
}
