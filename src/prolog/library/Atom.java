// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Predicate;
import prolog.constants.PrologAtom;
import prolog.constants.PrologAtomLike;
import prolog.constants.PrologInteger;
import prolog.exceptions.PrologInstantiationError;
import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.expressions.Term;
import prolog.unification.Unifier;

import java.math.BigInteger;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Atom manipulation predicates.
 */
public final class Atom {
    private Atom() {
        // Static methods/fields only
    }

    /**
     * True if atom is of given length.
     *
     * @param environment Execution environment
     * @param atomTerm Atom to obtain length
     * @param lengthTerm Length of atom
     */
    @Predicate("atom_length")
    public static void atomLength(Environment environment, Term atomTerm, Term lengthTerm) {
        if (!atomTerm.isInstantiated()) {
            throw PrologInstantiationError.error(environment, atomTerm);
        }
        PrologAtomLike atom = PrologAtomLike.from(atomTerm);
        if (lengthTerm.isInstantiated()) {
            BigInteger intLen = PrologInteger.from(lengthTerm).get();
            if (intLen.compareTo(BigInteger.valueOf(atom.name().length())) != 0) {
                environment.backtrack();
            }
            return;
        }
        PrologInteger length = new PrologInteger(BigInteger.valueOf(atom.name().length()));
        if (!Unifier.unify(environment.getLocalContext(), lengthTerm, length)) {
            environment.backtrack();
        }
    }

    /**
     * Either (a) concatenate left/right, (b) test left/right, (c) split into possible left/right
     * @param environment Execution environment
     * @param leftTerm Left atom term
     * @param rightTerm Right atom term
     * @param concatTerm Concatinated form
     */
    @Predicate("atom_concat")
    public static void atomConcat(Environment environment, Term leftTerm, Term rightTerm, Term concatTerm) {
        String leftString = null;
        String rightString = null;
        String concatString = null;
        if (leftTerm.isInstantiated()) {
            leftString = PrologAtomLike.from(leftTerm).name();
        }
        if (rightTerm.isInstantiated()) {
            rightString = PrologAtomLike.from(rightTerm).name();
        }
        if (concatTerm.isInstantiated()) {
            concatString = PrologAtomLike.from(concatTerm).name();
        }
        if (leftString != null && rightString != null) {
            if (!Unifier.unify(environment.getLocalContext(), concatTerm,
                    new PrologAtom(leftString + rightString))) {
                environment.backtrack();
            }
            return;
        }
        if (leftString != null && concatString != null) {
            // rightTerm is uninstantiated
            if (leftString.length() <= concatString.length() &&
                    concatString.substring(0, leftString.length()).equals(leftString)) {
                Unifier.unify(environment.getLocalContext(), rightTerm,
                        new PrologAtom(concatString.substring(leftString.length())));
            } else {
                environment.backtrack();
            }
            return;
        }
        if (rightString != null && concatString != null) {
            // leftTerm is uninstantiated
            int off = concatString.length() - rightString.length();
            if (rightString.length() <= concatString.length() &&
                    concatString.substring(off).equals(rightString)) {
                Unifier.unify(environment.getLocalContext(), leftTerm,
                        new PrologAtom(concatString.substring(0, off)));
            } else {
                environment.backtrack();
            }
            return;
        }
        if (concatString != null) {
            // Final case enumerates all possible permutations
            new AtomConcat(environment, concatString, leftTerm, rightTerm).next();
            return;
        }
        throw PrologInstantiationError.error(environment, concatTerm);
    }

    private static class AtomConcat extends DecisionPoint {

        private final String concat;
        private final Term leftTerm;
        private final Term rightTerm;
        private int split = 0;

        protected AtomConcat(Environment environment, String concat, Term leftTerm, Term rightTerm) {
            super(environment);
            this.concat = concat;
            this.leftTerm = leftTerm;
            this.rightTerm = rightTerm;
        }

        @Override
        protected void next() {
            if (split > concat.length()) {
                environment.backtrack();
                return;
            }
            environment.forward();
            if (split < concat.length()) {
                environment.pushDecisionPoint(this);
            }
            LocalContext context = environment.getLocalContext();
            PrologAtom leftAtom = new PrologAtom(concat.substring(0, split));
            PrologAtom rightAtom = new PrologAtom(concat.substring(split));
            split++;
            if (!(Unifier.unify(context, leftTerm, leftAtom) &&
                    Unifier.unify(context, rightTerm, rightAtom))) {
                environment.backtrack();
            }
        }
    }
}
