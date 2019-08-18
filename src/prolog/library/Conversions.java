// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Predicate;
import prolog.constants.PrologAtom;
import prolog.constants.PrologAtomLike;
import prolog.constants.PrologChars;
import prolog.constants.PrologCodePoints;
import prolog.constants.PrologEOF;
import prolog.constants.PrologEmptyList;
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
import java.nio.charset.StandardCharsets;
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
     * Common code for atom_chars and atom_codes
     *
     * @param environment Execution environment
     * @param atom        Atom to create or deconstruct
     * @param list        Target list
     * @param allocator   Defines how the deconstructed atom behaves
     */
    protected static void atomCharsCommon(Environment environment, Term atom, Term list, Function<String, Term> allocator) {
        if (!atom.isInstantiated()) {
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
        Term parsedNumber = null;
        if (list.isGrounded()) {
            // construct number through parsing
            String text = TermList.extractString(environment, list);
            Term t = parseSingleTerm(environment, text);
            if (!t.isNumber()) {
                throw PrologSyntaxError.tokenError(environment, "Expected to parse a number");
            }
            parsedNumber = t;
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

    /**
     * Convert string to term via parser
     *
     * @param environment Execution environment
     * @param text        Text to convert
     * @return Term
     */
    private static Term parseSingleTerm(Environment environment, String text) {
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
        Term single = tok.nextToken();
        Term end = tok.nextToken();
        if (end != PrologEOF.EOF) {
            throw PrologSyntaxError.tokenError(environment, "String was parsed as more than one token");
        }
        return single;
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
