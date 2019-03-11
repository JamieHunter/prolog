// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.expressions;

import prolog.constants.Atomic;
import prolog.constants.PrologAtom;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.unification.UnifyIterator;

/**
 * Compound term. A compound term (usually) consists of an atom, and one or more components. While a functor is an atom
 * in Prolog, we permit it to be any Atomic value internally. We also permit a compound term with no members as a
 * zero-arity predicate head.
 */
public interface CompoundTerm extends Term {

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
    static boolean termIsA(Term term, PrologAtom atom) {
        return term instanceof CompoundTerm && ((CompoundTerm) term).functor() == atom;
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
    static boolean termIsA(Term term, PrologAtom atom, int arity) {
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
    Atomic functor();

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
}
