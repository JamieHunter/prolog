// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Predicate;
import prolog.constants.PrologAtom;
import prolog.constants.PrologAtomLike;
import prolog.constants.PrologCharacter;
import prolog.constants.PrologChars;
import prolog.constants.PrologCodePoints;
import prolog.constants.PrologEOF;
import prolog.constants.PrologEmptyList;
import prolog.constants.PrologInteger;
import prolog.constants.PrologNumber;
import prolog.exceptions.PrologError;
import prolog.exceptions.PrologInstantiationError;
import prolog.exceptions.PrologSyntaxError;
import prolog.exceptions.PrologTypeError;
import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.expressions.TermList;
import prolog.flags.ReadOptions;
import prolog.flags.WriteOptions;
import prolog.io.InputBuffered;
import prolog.io.InputDecoderFilter;
import prolog.io.OutputEncoderFilter;
import prolog.io.PrologInputStream;
import prolog.io.PrologOutputStream;
import prolog.io.SequentialInputStream;
import prolog.io.SequentialOutputStream;
import prolog.io.WriteContext;
import prolog.parser.Tokenizer;
import prolog.unification.Unifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Conversions {

    /**
     * Construct, deconstruct or compare an atom with array of atom-characters
     *
     * @param environment Execution environment
     */
    @Predicate("atom_chars")
    public static void atomChars(Environment environment, Term atom, Term list) {
        atomCharsCommon(environment, atom, list, PrologChars::new);
    }

    /**
     * Construct, deconstruct or compare an atom with array of atom-characters
     *
     * @param environment Execution environment
     */
    @Predicate("atom_codes")
    public static void atomCodes(Environment environment, Term atom, Term list) {
        atomCharsCommon(environment, atom, list, PrologCodePoints::new);
    }

    /**
     * COnvert between atom character and character code
     * @param environment Execution environment
     * @param atomTerm Atom character
     * @param codeTerm Code representation
     */
    @Predicate("char_code")
    public static void charCode(Environment environment, Term atomTerm, Term codeTerm) {
        if (atomTerm.isInstantiated()) {
            String charString = "";

            if (atomTerm.isAtom()) {
                charString = PrologAtomLike.from(atomTerm).name();
            }
            if (charString.length() != 1) {
                throw PrologTypeError.characterExpected(environment, atomTerm);
            }
            PrologInteger code = PrologInteger.from(charString.charAt(0));
            if(!Unifier.unify(environment.getLocalContext(), codeTerm, code)) {
                environment.backtrack();
            }
            return;
        } else if (codeTerm.isInstantiated()) {
            int code = PrologInteger.from(codeTerm).toChar();
            PrologCharacter chr = new PrologCharacter((char)code);
            if(!Unifier.unify(environment.getLocalContext(), atomTerm, chr)) {
                environment.backtrack();
            }
            return;
        } else {
            throw PrologInstantiationError.error(environment, atomTerm);
        }
    }

    /**
     * Common code for atom_chars and atom_codes
     *
     * @param environment Execution environment
     * @param atom        Atom to create or deconstruct
     * @param list        Target list
     * @param allocator   Defines how the deconstructed atom behaves
     */
    protected static void atomCharsCommon(Environment environment, Term atom, Term list, Function<String, Term> allocator) {
        if (!atom.isInstantiated()) {
            if (!list.isInstantiated()) {
                throw PrologInstantiationError.error(environment, atom);
            }
            // construct atom, list must be grounded
            String text = TermList.extractString(environment, list);
            PrologAtom newAtom = new PrologAtom(text);
            if (!Unifier.unify(environment.getLocalContext(), atom, newAtom)) {
                environment.backtrack();
            }
            return;
        }
        PrologAtomLike realAtom = PrologAtomLike.from(atom);
        Term deconstructed;
        if (realAtom.name().length() == 0) {
            deconstructed = PrologEmptyList.EMPTY_LIST;
        } else {
            deconstructed = allocator.apply(realAtom.name());
        }
        if (!Unifier.unify(environment.getLocalContext(), list, deconstructed)) {
            environment.backtrack();
        }
    }

    /**
     * Construct, deconstruct or compare a number with representation as array of atom-characters
     *
     * @param environment Execution environment
     * @param number      Term representing a number2
     * @param list        List to compare/etc
     */
    @Predicate("number_chars")
    public static void numberChars(Environment environment, Term number, Term list) {
        numberCharsCommon(environment, number, list, PrologChars::new);
    }

    /**
     * Construct, deconstruct or compare a number with representation as array of character codes
     *
     * @param environment Execution environment
     * @param number      Term representing a number2
     * @param list        List to compare/etc
     */
    @Predicate("number_codes")
    public static void numberCodes(Environment environment, Term number, Term list) {
        numberCharsCommon(environment, number, list, PrologCodePoints::new);
    }


    /**
     * Common code for atom_chars and atom_codes
     *
     * @param environment Execution environment
     * @param number      Number to create or deconstruct
     * @param list        Target list
     * @param allocator   Defines how the deconstructed atom behaves
     */
    protected static void numberCharsCommon(Environment environment, Term number, Term list, Function<String, Term> allocator) {
        PrologNumber parsedNumber = null;
        if (list.isGrounded()) {
            // construct number through parsing
            String text = TermList.extractString(environment, list);
            try {
                parsedNumber = parseAndFoldInput(environment, text, new ParseAndFold<PrologNumber>() {
                    long sign = 0;
                    PrologNumber value = null;

                    @Override
                    public boolean accept(Term term) {
                        if (term.isAtom()) {
                            if (sign != 0) {
                                throw PrologSyntaxError.tokenError(environment, "sign");
                            }
                            String val = PrologAtomLike.from(term).name();
                            if (val.equals("-")) {
                                sign = -1;
                            } else if (val.equals("+")) {
                                sign = 1;
                            } else {
                                throw PrologSyntaxError.tokenError(environment, "sign");
                            }
                            return false; // keep parsing
                        } else if (term.isNumber()) {
                            value = (PrologNumber) term;
                            if (sign < 0) {
                                value = value.negate();
                            }
                            return true; // done parsing
                        } else {
                            throw PrologSyntaxError.tokenError(environment, "other");
                        }
                    }

                    @Override
                    public PrologNumber get() {
                        return value;
                    }
                });
            } catch(PrologError pe) {
                throw PrologSyntaxError.tokenError(environment, "Cannot parse " + text + " as a number");
            }
        }
        if (number.isInstantiated()) {
            if (!number.isNumber()) {
                throw PrologTypeError.numberExpected(environment, number);
            }
            if (parsedNumber == null) {
                // write number and unify
                String text = termToString(environment, number);
                Term deconstructed = allocator.apply(text);
                if (!Unifier.unify(environment.getLocalContext(), list, deconstructed)) {
                    environment.backtrack();
                }
                return;
            }
        } else if (parsedNumber == null) {
            throw PrologInstantiationError.error(environment, list);
        }
        if (!Unifier.unify(environment.getLocalContext(), number, parsedNumber)) {
            environment.backtrack();
        }
    }

    private interface ParseAndFold<R> {
        /**
         * Accept next term
         * @param term Term read from input, guaranteed to not be end-of-file
         * @return true if parsing complete, false otherwise
         */
        boolean accept(Term term);

        /**
         * @return parsed value
         */
        R get();
    }

    /**
     * Convert string to term via parser
     *
     * @param environment Execution environment
     * @param text        Text to convert
     * @param helper     Class to accept terms to generate a parsed value
     * @param <R>           Return type
     * @return resulting value
     */
    private static <R> R parseAndFoldInput(Environment environment,
                                           String text,
                                           ParseAndFold<R> helper) {
        PrologInputStream stream =
                new InputBuffered(
                        new InputDecoderFilter(
                                new SequentialInputStream(
                                        new ByteArrayInputStream(text.getBytes())
                                ), StandardCharsets.UTF_8), -1);
        Tokenizer tok = new Tokenizer(
                environment,
                new ReadOptions(environment, null),
                stream);
        boolean done = false;
        for(;;) {
            Term term = tok.nextToken();
            if (term == PrologEOF.EOF) {
                if (done) {
                    return helper.get();
                }
                break;
            } else {
                if (done) {
                    break;
                }
            }
            done = helper.accept(term);
        }
        throw PrologSyntaxError.tokenError(environment, "String parsing error");
    }

    /**
     * Convert Term to String using writer
     *
     * @param environment Execution environment
     * @param term        Term to convert
     * @return string form of single term
     */
    private static String termToString(Environment environment, Term term) {
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        PrologOutputStream stream =
                new OutputEncoderFilter(
                        new SequentialOutputStream(
                                byteArray
                        ), StandardCharsets.UTF_8);
        WriteOptions options = new WriteOptions(environment, null);
        options.quoted = false;
        WriteContext context = new WriteContext(environment, options, stream);
        try {
            term.write(context);
        } catch (IOException e) {
            throw new InternalError("Unexpected IO error", e);
        }
        return new String(byteArray.toByteArray(), StandardCharsets.UTF_8);
    }
}
