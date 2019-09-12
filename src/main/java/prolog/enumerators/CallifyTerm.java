// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.enumerators;

import prolog.bootstrap.Interned;
import prolog.constants.AtomicBase;
import prolog.constants.PrologAtomInterned;
import prolog.constants.PrologAtomLike;
import prolog.enumerators.EnumTermStrategy;
import prolog.exceptions.PrologTypeError;
import prolog.execution.Environment;
import prolog.expressions.CompoundTerm;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;
import prolog.variables.Variable;

import java.util.Optional;

/**
 * Strategy to process a term that is about to be called, or about to be asserted into the dictionary. Atoms are
 * interned. Atoms and Compound Terms are accepted as is (although may recurse). Variables are converted to a Call.
 * Others result in a syntax error. Note that variables cannot be re-labeled.
 */
public class CallifyTerm extends EnumTermStrategy {

    private boolean trimmed = false;
    private final Optional<Term> originalTerm;

    public CallifyTerm(Environment environment, Optional<Term> originalTerm) {
        super(environment);
        this.originalTerm = originalTerm;
    }

    /**
     * @return false to indicate grounded compound terms should be enumerated (for validation).
     */
    @Override
    public boolean pruneGroundedCompound() {
        return false;
    }

    /**
     * Intern atom on visit
     *
     * @param atom Atom being visited
     * @return interned atom
     */
    @Override
    public Term visitAtom(PrologAtomLike atom) {
        return PrologAtomInterned.from(environment(), atom);
    }

    /**
     * Non-atom atomics in a call position are an error
     *
     * @param atomic Atomic value being visited
     * @return self if allowed
     */
    @Override
    public Term visitAtomic(AtomicBase atomic) {
        if (trimmed) {
            return atomic;
        } else {
            throw PrologTypeError.callableExpected(environment(), originalTerm.orElse(atomic));
        }
    }

    /**
     * Compound terms are handled depending on where interpreted
     *
     * @param compound Compound value being visited
     * @return self or modified term
     */
    public CompoundTerm visitCompoundTerm(CompoundTerm compound) {
        PrologAtomInterned functor = PrologAtomInterned.from(environment(), compound.functor());
        if (trimmed) {
            return compound;
        } else {
            if (functor != Interned.COMMA_FUNCTOR && functor != Interned.SEMICOLON_FUNCTOR) {
                // stop translating CALL? TODO: verify any other constructs
                trimmed = true;
            }
            CompoundTerm newTerm = compound.enumAndCopyCompoundTermMembers(this);
            trimmed = false;
            return newTerm;
        }
    }

    /**
     * Wrap variables in call.
     *
     * @param variable Variable being visited
     * @return self or modified term
     */
    @Override
    public Term visitVariable(Variable variable) {
        if (trimmed) {
            return variable;
        } else {
            return new CompoundTermImpl(Interned.CALL_FUNCTOR, unbindVariable(variable));
        }
    }

}
