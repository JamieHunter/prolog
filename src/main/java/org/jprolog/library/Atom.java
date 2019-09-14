// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Predicate;
import org.jprolog.constants.PrologAtom;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.constants.PrologInteger;
import org.jprolog.exceptions.PrologInstantiationError;
import org.jprolog.execution.DecisionPointImpl;
import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.Term;
import org.jprolog.unification.Unifier;
import org.jprolog.flags.ReadOptions;
import org.jprolog.parser.StringParser;

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
     * @param atomTerm    Atom to obtain length
     * @param lengthTerm  Length of atom
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
        PrologInteger length = PrologInteger.from(atom.name().length());
        if (!Unifier.unify(environment.getLocalContext(), lengthTerm, length)) {
            environment.backtrack();
        }
    }

    /**
     * Utility to parse an atom as a term
     *
     * @param environment Execution environment
     * @param atomTerm    Source atom
     * @param outTerm     Extracted term after parsing
     * @param optionsTerm Parsing options
     */
    @Predicate("read_term_from_atom")
    public static void readTermFromAtom(Environment environment, Term atomTerm, Term outTerm, Term optionsTerm) {
        if (!atomTerm.isInstantiated()) {
            throw PrologInstantiationError.error(environment, atomTerm);
        }
        String text = PrologAtomLike.from(atomTerm).name();
        ReadOptions readOptions = new ReadOptions(environment, optionsTerm);
        readOptions.fullStop = ReadOptions.FullStop.ATOM_optional;
        Term out = StringParser.parse(environment, text, readOptions);
        if (!Unifier.unify(environment.getLocalContext(), outTerm, out)) {
            environment.backtrack();
        }
    }

    /**
     * Either (a) concatenate left/right, (b) test left/right, (c) split into possible left/right
     *
     * @param environment Execution environment
     * @param leftTerm    Left atom term
     * @param rightTerm   Right atom term
     * @param concatTerm  Concatinated form
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
            new AtomConcat(environment, concatString, leftTerm, rightTerm).redo();
            return;
        }
        throw PrologInstantiationError.error(environment, concatTerm);
    }

    /**
     * Given sub_atom(+Atom, ?Before, ?Length, ?After, ?SubAtom), identify all possible SubAtom's, and/or all possible
     * Before/Length/After.
     *
     * @param environment Execution environment
     * @param atomTerm    Source atom, required
     * @param beforeTerm  Variable, or integer >= 0, number of characters before sub-atom
     * @param lengthTerm  Variable, or integer >=0, length of sub-atom
     * @param afterTerm   Variable, or integer >=0, number of characters after sub-atom
     * @param subAtomTerm Variable, or defined sub-atom
     */
    @Predicate("sub_atom")
    public static void atomConcat(Environment environment, Term atomTerm, Term beforeTerm, Term lengthTerm, Term afterTerm, Term subAtomTerm) {
        String atomString;
        if (atomTerm.isInstantiated()) {
            atomString = PrologAtomLike.from(atomTerm).name();
        } else {
            throw PrologInstantiationError.error(environment, atomTerm);
        }
        new SubAtom(environment, atomString, beforeTerm, lengthTerm, afterTerm, subAtomTerm).redo();
    }

    private static class AtomConcat extends DecisionPointImpl {

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
        public void redo() {
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

    private static class SubAtom extends DecisionPointImpl {
        private final String atomString;
        private final Term beforeTerm;
        private final Term lengthTerm;
        private final Term afterTerm;
        private final Term subAtomTerm;
        private Integer beforeConstraint;
        private Integer lengthConstraint;
        private Integer afterConstraint;
        private String subAtomConstraint;
        private int offset;
        private int length;
        private int limit;
        private Runnable algorithm = this::backtrack;

        /**
         * Create a new decision point associated with the environment. At time decision point is created, the local context,
         * the catch point, the cut depth and the call stack are all snapshot and reused on each iteration of the decision
         * point.
         *
         * @param environment Execution environment
         */
        protected SubAtom(Environment environment, String atomString, Term beforeTerm, Term lengthTerm, Term afterTerm, Term subAtomTerm) {
            super(environment);
            this.atomString = atomString;
            this.beforeTerm = beforeTerm;
            this.lengthTerm = lengthTerm;
            this.afterTerm = afterTerm;
            this.subAtomTerm = subAtomTerm;
            int atomLen = atomString.length();
            offset = -1; // to help identify errors in below logic
            length = -1;
            limit = -1;

            if (beforeTerm.isInstantiated()) {
                beforeConstraint = PrologInteger.from(beforeTerm).notLessThanZero().toInteger();
                if (beforeConstraint > atomLen) {
                    return; // not solvable
                }
            }
            if (lengthTerm.isInstantiated()) {
                lengthConstraint = PrologInteger.from(lengthTerm).notLessThanZero().toInteger();
                if (lengthConstraint > atomLen) {
                    return; // not solvable
                }
            }
            if (afterTerm.isInstantiated()) {
                // TODO: bug, integer needs to be bounded
                afterConstraint = PrologInteger.from(afterTerm).notLessThanZero().toInteger();
                if (afterConstraint > atomLen) {
                    return; // not solvable
                }
            }
            if (subAtomTerm.isInstantiated()) {
                subAtomConstraint = PrologAtomLike.from(subAtomTerm).name();
                if (lengthConstraint != null) {
                    if (lengthConstraint != subAtomConstraint.length()) {
                        return; // not solvable
                    }
                } else {
                    // implied length constraint
                    lengthConstraint = subAtomConstraint.length();
                }
            }
            // infer additional constraints
            if (beforeConstraint != null && lengthConstraint != null && afterConstraint == null) {
                afterConstraint = atomLen - (beforeConstraint + lengthConstraint);
                if (afterConstraint < 0 || afterConstraint > atomLen) {
                    return; // not solvable
                }
            }
            if (beforeConstraint != null && lengthConstraint == null && afterConstraint != null) {
                lengthConstraint = atomLen - (beforeConstraint + afterConstraint);
                if (lengthConstraint < 0 || lengthConstraint > atomLen) {
                    return; // not solvable
                }
            }
            if (beforeConstraint == null && lengthConstraint != null && afterConstraint != null) {
                beforeConstraint = atomLen - (afterConstraint + lengthConstraint);
                if (beforeConstraint < 0 || beforeConstraint > atomLen) {
                    return; // not solvable
                }
            }
            // Given the constraints (provided or inferred), determine the algorithm and starting condition
            if (beforeConstraint != null && lengthConstraint != null && afterConstraint != null) {
                int checkLen = beforeConstraint + lengthConstraint + afterConstraint;
                if (checkLen != atomLen) {
                    return; // not solvable
                }
                offset = limit = beforeConstraint;
                length = lengthConstraint;
                algorithm = this::fullyConstrained;
                return;
            }
            if (beforeConstraint == null && afterConstraint != null) {
                assert lengthConstraint == null;
                algorithm = this::enumerateFixedRight;
                offset = 0;
                length = atomLen - afterConstraint; // starting length
                limit = length;
                return;
            }
            if (beforeConstraint != null && afterConstraint == null) {
                assert lengthConstraint == null;
                algorithm = this::enumerateFixedLeft;
                offset = beforeConstraint;
                length = 0;
                limit = atomLen;
                return;
            }
            assert beforeConstraint == null;
            assert afterConstraint == null;
            offset = 0;
            if (lengthConstraint == null) {
                limit = atomLen;
                length = 0;
                algorithm = this::enumerateAll;
            } else if (lengthConstraint == 0) {
                limit = atomLen;
                length = 0;
                algorithm = this::scanEmpty;
            } else if (subAtomConstraint != null) {
                length = lengthConstraint;
                limit = atomLen - lengthConstraint;
                algorithm = this::scanString;
            } else {
                length = lengthConstraint;
                limit = atomLen - lengthConstraint;
                algorithm = this::enumerateFixedLength;
            }
        }

        /**
         * All constraints applied, this only runs once
         */
        protected void fullyConstrained() {
            String subAtom = atomString.substring(offset, offset + length);
            unify(offset, subAtom);
        }

        /**
         * Before is fixed, length and after are variable
         */
        protected void enumerateFixedLeft() {
            int end = offset + length;
            String subAtom = atomString.substring(offset, end);
            if (end != limit) {
                notLast();
            }
            unify(offset, subAtom);
            length++;
        }

        /**
         * After is fixed, length and before are variable
         */
        protected void enumerateFixedRight() {
            int end = offset + length;
            String subAtom = atomString.substring(offset, end);
            if (offset != limit) {
                notLast();
            }
            unify(offset, subAtom);
            offset++;
            length--;
        }

        /**
         * Length is fixed, before and after are variable
         */
        protected void enumerateFixedLength() {
            int end = offset + length;
            String subAtom = atomString.substring(offset, end);
            if (offset != limit) {
                notLast();
            }
            unify(offset, subAtom);
            offset++;
        }

        /**
         * Completely unconstrained
         */
        protected void enumerateAll() {
            int end = offset + length;
            String subAtom = atomString.substring(offset, end);
            if (offset != limit) {
                notLast();
            }
            unify(offset, subAtom);
            if (end == limit) {
                offset++;
                length = 0;
            } else {
                length++;
            }
        }

        /**
         * Algorithm: start/end constraints are empty,
         * String is empty string
         */
        protected void scanEmpty() {
            if (offset == limit + 1) {
                forceBacktrack();
                return;
            }
            if (offset != limit) {
                notLast();
            }
            unify(offset, "");
            offset++;
        }

        /**
         * Algorithm: start/end constraints are empty,
         * String is a provided string.
         */
        protected void scanString() {
            for (; ; ) {
                if (!scan()) {
                    forceBacktrack();
                    return;
                }
                if (atomString.substring(offset, offset + length).equals(subAtomConstraint)) {
                    break;
                }
                offset++;
            }
            if (offset < limit) {
                notLast();
            }
            unify(offset, subAtomConstraint);
            offset++;
        }

        /**
         * Helper for the scanString algorithm, find first character
         *
         * @return true if first character found
         */
        protected boolean scan() {
            char c = subAtomConstraint.charAt(0);
            while (offset <= limit) {
                if (atomString.charAt(offset) == c) {
                    return true;
                }
                offset++;
            }
            return false;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void redo() {
            environment.forward();
            algorithm.run();
        }

        /**
         * Algorithm: Determined we've already failed
         */
        protected void forceBacktrack() {
            environment.backtrack();
        }

        /**
         * Called to push decision point
         */
        protected void notLast() {
            environment.pushDecisionPoint(this);
        }

        protected void unify(int before, String subAtom) {
            int length = subAtom.length();
            int after = atomString.length() - (length + before);
            if (!beforeTerm.isInstantiated()) {
                if (!beforeTerm.instantiate(PrologInteger.from(before))) {
                    forceBacktrack();
                    return;
                }
            }
            if (!lengthTerm.isInstantiated()) {
                if (!lengthTerm.instantiate(PrologInteger.from(length))) {
                    forceBacktrack();
                    return;
                }
            }
            if (!afterTerm.isInstantiated()) {
                if (!afterTerm.instantiate(PrologInteger.from(after))) {
                    forceBacktrack();
                    return;
                }
            }
            if (!subAtomTerm.isInstantiated()) {
                if (!subAtomTerm.instantiate(new PrologAtom(subAtom))) {
                    forceBacktrack();
                    return;
                }
            }
        }

    }
}
