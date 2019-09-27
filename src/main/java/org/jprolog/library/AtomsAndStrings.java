// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Predicate;
import org.jprolog.constants.PrologAtom;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.constants.PrologInteger;
import org.jprolog.constants.PrologString;
import org.jprolog.exceptions.PrologInstantiationError;
import org.jprolog.execution.DecisionPointImpl;
import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.Strings;
import org.jprolog.expressions.Term;
import org.jprolog.flags.ReadOptions;
import org.jprolog.flags.WriteOptions;
import org.jprolog.io.StructureWriter;
import org.jprolog.parser.StringParser;
import org.jprolog.unification.Unifier;

import java.math.BigInteger;
import java.util.function.Function;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Atom manipulation predicates.
 */
public final class AtomsAndStrings {
    private AtomsAndStrings() {
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
        commonLength(environment, atomTerm, lengthTerm, t -> PrologAtomLike.from(t).name());
    }

    /**
     * True if string is of given length.
     *
     * @param environment Execution environment
     * @param stringTerm  String to obtain length
     * @param lengthTerm  Length of string
     */
    @Predicate("string_length")
    public static void stringLength(Environment environment, Term stringTerm, Term lengthTerm) {
        commonLength(environment, stringTerm, lengthTerm, Strings::stringFromAnyString);
    }

    public static void commonLength(Environment environment, Term sourceTerm, Term lengthTerm, Function<Term,String> extract) {
        if (!sourceTerm.isInstantiated()) {
            throw PrologInstantiationError.error(environment, sourceTerm);
        }
        String text = extract.apply(sourceTerm);
        if (lengthTerm.isInstantiated()) {
            BigInteger intLen = PrologInteger.from(lengthTerm).get();
            if (intLen.compareTo(BigInteger.valueOf(text.length())) != 0) {
                environment.backtrack();
            }
            return;
        }
        PrologInteger length = PrologInteger.from(text.length());
        if (!Unifier.unify(environment.getLocalContext(), lengthTerm, length)) {
            environment.backtrack();
        }
    }

    /**
     * Utility to parse an string as a term or convert a term to a string
     *
     * @param environment Execution environment
     * @param term        Source/target term
     * @param stringTerm  String to convert to/from
     * @param optionsTerm Parsing options
     */
    @Predicate("term_string")
    public static void termString(Environment environment, Term term, Term stringTerm, Term optionsTerm) {
        if (stringTerm.isInstantiated()) {
            stringToTerm(environment, stringTerm, term, new ReadOptions(environment, ro -> {
                ro.fullStop = ReadOptions.FullStop.ATOM_optional;
            }, optionsTerm));
        } else {
            if (!term.isInstantiated()) {
                throw PrologInstantiationError.error(environment, term);
            }
            termToString(environment, term, stringTerm, new WriteOptions(environment, wo -> {
                wo.quoted = true;
            }, optionsTerm));
        }
    }

    /**
     * Utility to parse an string as a term or convert a term to a string
     *
     * @param environment Execution environment
     * @param term        Source/target term
     * @param stringTerm  String to convert to/from
     */
    @Predicate("term_string")
    public static void termString(Environment environment, Term term, Term stringTerm) {
        termString(environment, term, stringTerm, null);
    }

    public static void stringToTerm(Environment environment, Term stringTerm, Term outTerm, ReadOptions options) {
        String text = Strings.stringFromAtomOrAnyString(stringTerm);
        Term out = StringParser.parse(environment, text, options);
        if (!Unifier.unify(environment.getLocalContext(), outTerm, out)) {
            environment.backtrack();
        }
    }

    public static void termToString(Environment environment, Term inTerm, Term stringTerm, WriteOptions options) {
        String text = StructureWriter.toString(environment, inTerm, options);
        if (!Unifier.unify(environment.getLocalContext(), stringTerm, new PrologString(text))) {
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
        ReadOptions readOptions = new ReadOptions(environment, ro -> {
            ro.fullStop = ReadOptions.FullStop.ATOM_optional;
        }, optionsTerm);
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
        concatCommon(environment, leftTerm, rightTerm, concatTerm, t -> PrologAtomLike.from(t).name(), PrologAtom::new);
    }

    /**
     * Either (a) concatenate left/right, (b) test left/right, (c) split into possible left/right
     *
     * @param environment Execution environment
     * @param leftTerm    Left string term
     * @param rightTerm   Right string term
     * @param concatTerm  Concatinated form
     */
    @Predicate("string_concat")
    public static void stringConcat(Environment environment, Term leftTerm, Term rightTerm, Term concatTerm) {
        concatCommon(environment, leftTerm, rightTerm, concatTerm, Strings::stringFromAnyString, PrologString::new);
    }

    /**
     * Generic form of concat
     *
     * @param environment Execution environment
     * @param leftTerm    Left term
     * @param rightTerm   Right term
     * @param concatTerm  Concatinated form
     * @param extract     Extract term to a string
     * @param create      Create term from a string
     */
    private static void concatCommon(Environment environment, Term leftTerm, Term rightTerm, Term concatTerm,
                                     Function<Term, String> extract, Function<String, Term> create) {
        String leftString = null;
        String rightString = null;
        String concatString = null;
        if (leftTerm.isInstantiated()) {
            leftString = extract.apply(leftTerm);
        }
        if (rightTerm.isInstantiated()) {
            rightString = extract.apply(rightTerm);
        }
        if (concatTerm.isInstantiated()) {
            concatString = extract.apply(concatTerm);
        }
        if (leftString != null && rightString != null) {
            if (!Unifier.unify(environment.getLocalContext(), concatTerm,
                    create.apply(leftString + rightString))) {
                environment.backtrack();
            }
            return;
        }
        if (leftString != null && concatString != null) {
            // rightTerm is uninstantiated
            if (leftString.length() <= concatString.length() &&
                    concatString.substring(0, leftString.length()).equals(leftString)) {
                Unifier.unify(environment.getLocalContext(), rightTerm,
                        create.apply(concatString.substring(leftString.length())));
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
                        create.apply(concatString.substring(0, off)));
            } else {
                environment.backtrack();
            }
            return;
        }
        if (concatString != null) {
            // Final case enumerates all possible permutations
            new Concat(environment, concatString, leftTerm, rightTerm, create).redo();
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
    public static void subAtom(Environment environment, Term atomTerm, Term beforeTerm, Term lengthTerm, Term afterTerm, Term subAtomTerm) {
        String atomString;
        if (atomTerm.isInstantiated()) {
            atomString = PrologAtomLike.from(atomTerm).name();
        } else {
            throw PrologInstantiationError.error(environment, atomTerm);
        }
        new Sub(environment, atomString, beforeTerm, lengthTerm, afterTerm, subAtomTerm,
                t -> PrologAtomLike.from(t).name(), PrologAtom::new).redo();
    }

    /**
     * Given sub_string(+String, ?Before, ?Length, ?After, ?SubString), identify all possible SubString's, and/or all possible
     * Before/Length/After.
     *
     * @param environment   Execution environment
     * @param stringTerm    Source atom, required
     * @param beforeTerm    Variable, or integer >= 0, number of characters before sub-atom
     * @param lengthTerm    Variable, or integer >=0, length of sub-atom
     * @param afterTerm     Variable, or integer >=0, number of characters after sub-atom
     * @param subStringTerm Variable, or defined sub-atom
     */
    @Predicate("sub_string")
    public static void subSting(Environment environment, Term stringTerm, Term beforeTerm, Term lengthTerm, Term afterTerm, Term subStringTerm) {
        String sourceString;
        if (stringTerm.isInstantiated()) {
            sourceString = Strings.stringFromAnyString(stringTerm);
        } else {
            throw PrologInstantiationError.error(environment, stringTerm);
        }
        new Sub(environment, sourceString, beforeTerm, lengthTerm, afterTerm, subStringTerm,
                Strings::stringFromAnyString, PrologString::new).redo();
    }

    private static class Concat extends DecisionPointImpl {

        private final String concat;
        private final Term leftTerm;
        private final Term rightTerm;
        private final Function<String, Term> create;
        private int split = 0;

        protected Concat(Environment environment, String concat, Term leftTerm, Term rightTerm, Function<String, Term> create) {
            super(environment);
            this.concat = concat;
            this.leftTerm = leftTerm;
            this.rightTerm = rightTerm;
            this.create = create;
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
            Term leftNew = create.apply(concat.substring(0, split));
            Term rightNew = create.apply(concat.substring(split));
            split++;
            if (!(Unifier.unify(context, leftTerm, leftNew) &&
                    Unifier.unify(context, rightTerm, rightNew))) {
                environment.backtrack();
            }
        }
    }

    private static class Sub extends DecisionPointImpl {
        private final String sourceString;
        private final Term beforeTerm;
        private final Term lengthTerm;
        private final Term afterTerm;
        private final Term subTerm;
        private Integer beforeConstraint;
        private Integer lengthConstraint;
        private Integer afterConstraint;
        private String subConstraint;
        private int offset;
        private int length;
        private int limit;
        private Runnable algorithm = this::backtrack;
        private final Function<String, Term> create;

        /**
         * Create a new decision point associated with the environment. At time decision point is created, the local context,
         * the catch point, the cut depth and the call stack are all snapshot and reused on each iteration of the decision
         * point.
         *
         * @param environment Execution environment
         */
        protected Sub(Environment environment, String sourceString, Term beforeTerm, Term lengthTerm, Term afterTerm, Term subTerm,
                      Function<Term, String> extract,
                      Function<String, Term> create) {
            super(environment);
            this.sourceString = sourceString;
            this.beforeTerm = beforeTerm;
            this.lengthTerm = lengthTerm;
            this.afterTerm = afterTerm;
            this.subTerm = subTerm;
            this.create = create;
            int sourceLength = sourceString.length();
            offset = -1; // to help identify errors in below logic
            length = -1;
            limit = -1;

            if (beforeTerm.isInstantiated()) {
                beforeConstraint = PrologInteger.from(beforeTerm).notLessThanZero().toInteger();
                if (beforeConstraint > sourceLength) {
                    return; // not solvable
                }
            }
            if (lengthTerm.isInstantiated()) {
                lengthConstraint = PrologInteger.from(lengthTerm).notLessThanZero().toInteger();
                if (lengthConstraint > sourceLength) {
                    return; // not solvable
                }
            }
            if (afterTerm.isInstantiated()) {
                afterConstraint = PrologInteger.from(afterTerm).notLessThanZero().toInteger();
                if (afterConstraint > sourceLength) {
                    return; // not solvable
                }
            }
            if (subTerm.isInstantiated()) {
                subConstraint = extract.apply(subTerm);
                if (lengthConstraint != null) {
                    if (lengthConstraint != subConstraint.length()) {
                        return; // not solvable
                    }
                } else {
                    // implied length constraint
                    lengthConstraint = subConstraint.length();
                }
            }
            // infer additional constraints
            if (beforeConstraint != null && lengthConstraint != null && afterConstraint == null) {
                afterConstraint = sourceLength - (beforeConstraint + lengthConstraint);
                if (afterConstraint < 0 || afterConstraint > sourceLength) {
                    return; // not solvable
                }
            }
            if (beforeConstraint != null && lengthConstraint == null && afterConstraint != null) {
                lengthConstraint = sourceLength - (beforeConstraint + afterConstraint);
                if (lengthConstraint < 0 || lengthConstraint > sourceLength) {
                    return; // not solvable
                }
            }
            if (beforeConstraint == null && lengthConstraint != null && afterConstraint != null) {
                beforeConstraint = sourceLength - (afterConstraint + lengthConstraint);
                if (beforeConstraint < 0 || beforeConstraint > sourceLength) {
                    return; // not solvable
                }
            }
            // Given the constraints (provided or inferred), determine the algorithm and starting condition
            if (beforeConstraint != null && lengthConstraint != null && afterConstraint != null) {
                int checkLen = beforeConstraint + lengthConstraint + afterConstraint;
                if (checkLen != sourceLength) {
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
                length = sourceLength - afterConstraint; // starting length
                limit = length;
                return;
            }
            if (beforeConstraint != null && afterConstraint == null) {
                assert lengthConstraint == null;
                algorithm = this::enumerateFixedLeft;
                offset = beforeConstraint;
                length = 0;
                limit = sourceLength;
                return;
            }
            assert beforeConstraint == null;
            assert afterConstraint == null;
            offset = 0;
            if (lengthConstraint == null) {
                limit = sourceLength;
                length = 0;
                algorithm = this::enumerateAll;
            } else if (lengthConstraint == 0) {
                limit = sourceLength;
                length = 0;
                algorithm = this::scanEmpty;
            } else if (subConstraint != null) {
                length = lengthConstraint;
                limit = sourceLength - lengthConstraint;
                algorithm = this::scanString;
            } else {
                length = lengthConstraint;
                limit = sourceLength - lengthConstraint;
                algorithm = this::enumerateFixedLength;
            }
        }

        /**
         * All constraints applied, this only runs once
         */
        protected void fullyConstrained() {
            String sub = sourceString.substring(offset, offset + length);
            unify(offset, sub);
        }

        /**
         * Before is fixed, length and after are variable
         */
        protected void enumerateFixedLeft() {
            int end = offset + length;
            String sub = sourceString.substring(offset, end);
            if (end != limit) {
                notLast();
            }
            unify(offset, sub);
            length++;
        }

        /**
         * After is fixed, length and before are variable
         */
        protected void enumerateFixedRight() {
            int end = offset + length;
            String sub = sourceString.substring(offset, end);
            if (offset != limit) {
                notLast();
            }
            unify(offset, sub);
            offset++;
            length--;
        }

        /**
         * Length is fixed, before and after are variable
         */
        protected void enumerateFixedLength() {
            int end = offset + length;
            String sub = sourceString.substring(offset, end);
            if (offset != limit) {
                notLast();
            }
            unify(offset, sub);
            offset++;
        }

        /**
         * Completely unconstrained
         */
        protected void enumerateAll() {
            int end = offset + length;
            String sub = sourceString.substring(offset, end);
            if (offset != limit) {
                notLast();
            }
            unify(offset, sub);
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
                if (sourceString.substring(offset, offset + length).equals(subConstraint)) {
                    break;
                }
                offset++;
            }
            if (offset < limit) {
                notLast();
            }
            unify(offset, subConstraint);
            offset++;
        }

        /**
         * Helper for the scanString algorithm, find first character
         *
         * @return true if first character found
         */
        protected boolean scan() {
            char c = subConstraint.charAt(0);
            while (offset <= limit) {
                if (sourceString.charAt(offset) == c) {
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

        protected void unify(int before, String sub) {
            int length = sub.length();
            int after = sourceString.length() - (length + before);
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
            if (!subTerm.isInstantiated()) {
                if (!subTerm.instantiate(create.apply(sub))) {
                    forceBacktrack();
                    return;
                }
            }
        }

    }
}
