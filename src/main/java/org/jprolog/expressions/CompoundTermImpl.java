// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.expressions;

import org.jprolog.unification.SimpleUnifyIterator;
import org.jprolog.unification.UnifyIterator;
import org.jprolog.constants.Atomic;
import org.jprolog.execution.CompileContext;
import org.jprolog.enumerators.EnumTermStrategy;
import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.io.StructureWriter;
import org.jprolog.io.WriteContext;
import org.jprolog.predicates.PredicateDefinition;
import org.jprolog.predicates.Predication;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of a compound term.
 */
public class CompoundTermImpl implements CompoundTerm {

    // members[0] is functor, 1..n are arguments
    protected final Term[] members;

    /**
     * Construct {@link CompoundTerm} from functor and components. The term need not be grounded.
     *
     * @param functor   Functor. Strictly an Atom. Loosely Atomic.
     * @param arguments List of arguments. This is assumed to be at least one argument, but allows special case of none.
     */
    public CompoundTermImpl(Atomic functor, List<Term> arguments) {
        members = new Term[arguments.size() + 1];
        members[0] = functor;
        int i = 0;
        for (Term t : arguments) {
            members[++i] = t;
        }
    }

    /**
     * Construct a new compound term given a functor and an array of arguments.
     *
     * @param functor   Functor
     * @param arguments array of arguments
     */
    public CompoundTermImpl(Atomic functor, Term... arguments) {
        this(functor, Arrays.asList(arguments));
    }

    /**
     * Construct a new compound term given an array of members, first member is a functor, remaining members are
     * the compound term's arguments.
     *
     * @param members Array of terms
     */
    public CompoundTermImpl(Term[] members) {
        this.members = members.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(members[0]);
        builder.append('(');
        if (members.length > 1) {
            builder.append(members[1]);
        }
        for (int i = 2; i < members.length; i++) {
            builder.append(',');
            builder.append(members[i]);
        }
        builder.append(')');
        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int arity() {
        return members.length - 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Atomic functor() {
        return (Atomic) members[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term get(int i) {
        return members[i + 1];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnifyIterator getUnifyIterator() {
        return new SimpleUnifyIterator(arity(), members);
    }

    /**
     * Compile this compound term. This looks up dictionary entry and delegates compilation to the predicate
     * definition.
     *
     * @param compiling Context for compiling to target execution block.
     */
    @Override
    public void compile(CompileContext compiling) {
        Environment.Shared environmentShared = compiling.environmentShared();
        Predication predication = toPredication();
        PredicateDefinition definition = environmentShared.autoCreateDictionaryEntry(predication);
        definition.compile(predication, compiling, this);
    }

    /**
     * Resolves compound terms recursively. Term is initially assumed to not be grounded. However as a result of
     * the resolve, the term may be determined to be grounded (or may become grounded). If it is grounded, it will
     * be annotated as such (using the more efficient {@link GroundedCompoundTerm}). If it is not grounded, it will
     * be replaced by a new version that may be more resolved than the original.
     */
    @Override
    public CompoundTerm resolve(LocalContext context) {
        // Algorithm copies on change
        Term[] target = members;
        boolean changed = false;
        boolean grounded = true;
        for (int i = 0; i < members.length; i++) {
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
        if (grounded) {
            return new GroundedCompoundTerm(target);
        } else if (!changed) {
            return this;
        } else {
            return new CompoundTermImpl(target);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompoundTerm enumTerm(EnumTermStrategy strategy) {
        return strategy.visitCompoundTerm(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompoundTerm enumAndCopyCompoundTermMembers(EnumTermStrategy strategy) {
        return (CompoundTerm) strategy.computeUncachedTerm(this, tt -> {
            Term[] copy = members.clone();
            boolean grounded = true;
            for (int i = 0; i < copy.length; i++) {
                Term t = copy[i].enumTerm(strategy);
                grounded = grounded && t.isGrounded();
                copy[i] = t;
            }
            if (grounded) {
                return new GroundedCompoundTerm(copy);
            } else {
                return new CompoundTermImpl(copy);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompoundTerm enumCompoundTermMembers(EnumTermStrategy strategy) {
        for (Term t : members) {
            t.enumTerm(strategy);
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
}
