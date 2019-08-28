// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.expressions;

import prolog.constants.Atomic;
import prolog.constants.PrologAtomInterned;
import prolog.constants.PrologAtomLike;
import prolog.debugging.InstructionReflection;
import prolog.execution.EnumTermStrategy;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.unification.UnifyIterator;

/**
 * Compound term. A compound term (usually) consists of an atom, and one or more components. While a functor is an atom
 * in Prolog, we permit it to be any Atomic value internally. We also permit a compound term with no members as a
 * zero-arity predicate head.
 */
public interface CompoundTerm extends Term, InstructionReflection {

    /**
     * Utility, create a compound term consisting of only a functor. As the functor is Atomic, the compound term is
     * implicitly grounded.
     *
     * @param functor Functor
     * @return compound term
     */
    static CompoundTerm from(Atomic functor) {
        return new GroundedCompoundTerm((Atomic) functor);
    }

    /**
     * Utility to determine if specified term is compound, and is of a particular atom.
     *
     * @param term Term to test
     * @param atom Atom to match
     * @return true if matched
     */
    static boolean termIsA(Term term, PrologAtomInterned atom) {
        return term instanceof CompoundTerm && atom.compareTo(((CompoundTerm) term).functor()) == 0;
    }

    /**
     * Utility to determine if specified term is compound, and is of a particular atom,
     * and arity.
     *
     * @param term  Term to test
     * @param atom  Atom to match
     * @param arity Lambda to match
     * @return true if matched
     */
    static boolean termIsA(Term term, PrologAtomInterned atom, int arity) {
        return termIsA(term, atom) && ((CompoundTerm) term).arity() == arity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    CompoundTerm resolve(LocalContext context);

    /**
     * {@inheritDoc}
     */
    @Override
    CompoundTerm enumTerm(EnumTermStrategy strategy);

    /**
     * Called by {@link EnumTermStrategy#visit(CompoundTerm)} for mutation
     *
     * @param strategy Underlying strategy
     * @return new compound term
     */
    CompoundTerm mutateCompoundTerm(EnumTermStrategy strategy);

    /**
     * Called by {@link EnumTermStrategy#visit(CompoundTerm)} for simple enumeration
     *
     * @param strategy Underlying strategy
     * @return self
     */
    CompoundTerm enumCompoundTerm(EnumTermStrategy strategy);

    /**
     * {@inheritDoc}
     */
    @Override
    default CompoundTerm value(Environment environment) {
        return this;
    }

    /**
     * Number of components, 1 or more (0 reserved for atom)
     *
     * @return arity of compound term
     */
    int arity();

    /**
     * Functor of compound term
     *
     * @return Functor
     */
    PrologAtomLike functor();

    /**
     * Component of compound term at given index
     *
     * @param i Component index
     * @return Component
     */
    Term get(int i);

    /**
     * @return An iterator to use during unification
     */
    UnifyIterator getUnifyIterator();

    /**
     * {@inheritDoc}
     */
    @Override
    default int typeRank() {
        return TypeRank.COMPOUND_TERM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default int compareSameType(Term o) {
        // per standard, arity precedes functor in ordering
        CompoundTerm other = (CompoundTerm) o;
        int comp = Integer.compare(arity(), other.arity());
        if (comp != 0) {
            return comp;
        }
        comp = functor().compareTo(other.functor());
        if (comp != 0) {
            return comp;
        }
        int a = arity();
        for (int i = 0; i < a; i++) {
            comp = get(i).compareTo(other.get(i));
            if (comp != 0) {
                return comp;
            }
        }
        return comp;
    }

    /**
     * Reflection on a compound term is self
     * @return self
     */
    default CompoundTerm reflect() {
        return this;
    }
}
