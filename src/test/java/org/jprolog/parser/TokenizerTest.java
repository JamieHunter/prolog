package org.jprolog.parser;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jprolog.constants.PrologEOF;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.Term;
import org.jprolog.flags.PrologFlags;
import org.jprolog.flags.ReadOptions;
import org.jprolog.io.PrologInputStream;
import org.jprolog.test.StreamUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.jprolog.test.Matchers.*;

/**
 * Test the tokenizer, which is responsible for parsing the minimum Term without interpretation.
 */
@SuppressWarnings("OctalInteger")
public class TokenizerTest {

    private Environment environment;
    private ReadOptions readOptions;

    @BeforeEach
    private void init() {
        environment = new Environment();
        readOptions = new ReadOptions(environment, null);
    }

    private void expect(String text, Matcher<? super Term>... terms) {
        PrologInputStream stream = StreamUtils.bufferedString(text);
        Tokenizer tok = new Tokenizer(
                environment,
                readOptions,
                stream);
        for (int i = 0; i < terms.length; i++) {
            Term t = tok.nextToken();
            assertThat(t, terms[i]);
        }
        Term t = tok.nextToken();
        assertThat(t, is(PrologEOF.EOF));
    }

    /**
     * this test ensures navigation across a set of atoms.
     */
    @Test
    public void testSimpleSingleCharAtoms() {
        expect("a b c",
                isAtom("a"),
                isAtom("b"),
                isAtom("c"));
    }

    /**
     * This test looks for a richer set of operations.
     */
    @Test
    public void testSimpleSingleCharGraphics() {
        expect("a(bc,=..,12)*_",
                isAtom("a"),
                isAtom("("),
                isAtom("bc"),
                isAtom(","),
                isAtom("=.."),
                isAtom(","),
                isInteger(12),
                isAtom(")"),
                isAtom("*"),
                isUninstantiated());
    }

    /**
     * Ensure a string can be parsed. This also ensures that the cursor moves forward correctly.
     */
    @Test
    public void testParseSimpleString() {
        // String "Hello" followed immediately by the string """"
        expect("\"Hello\" \"\"\"\"",
                isString("Hello"),
                isString("\""));
    }

    @Test
    public void testComplexStrings() {
        expect("\"a\\001\\b\"", isString("a\001b"));
        expect("\"a\\x40\\b\"", isString("a@b"));
        expect("\"a\\001\\b\\x40\\c\"", isString("a\001b@c"));
    }

    @Test
    public void testDecimal() {
        expect("1234", isInteger(1234));
        expect("0b01110110", isInteger(0x76));
        expect("0o123", isInteger(0123));
        expect("0x123", isInteger(0x123));

        // With underscore
        expect("12_34", isInteger(1234));
    }

    @Test
    public void testCharCode() {
        expect("0'A", isInteger('A'));
    }

    @Test
    public void testFloat() {
        expect("12.0", isFloat(12.0)); // contrast to "12." test above
        expect("12_34.56e10", isFloat(1234.56e10));
    }

    public void testNotFloat() {
        // These look kind of like floats, but are not

        // Period after decimal is decimal not float
        expect("12.", isInteger(12), isAtom("."));

        // Likewise this is not a float
        expect(".12", isAtom("."), isInteger(12));

        // Underscores are tricky
        expect("_12.3", isUninstantiated(), isFloat(12.3));
        expect("12._3", isInteger(12), isAtom("."), isUninstantiated(), isInteger(3));
    }

    @Test
    public void testQuotedAtom() {
        expect("'123' 'abc'", isAtom("123"), isAtom("abc"));
        expect("'''' '\\'' '\\x41\\'", isAtom("'"), isAtom("'"), isAtom("A"));
    }

    @Test
    public void testAtomsSplitLines() {
        expect("x\ny\nz", isAtom("x"), isAtom("y"), isAtom("z"));
    }

    @Test
    public void testQuotedSplitLinesIgnored() {
        expect("'x\\\ny\\\nz'", isAtom("xyz"));
    }

    @Test
    public void testQuotedSplitLinesLiteral() {
        expect("'x\ny\nz'", isAtom("x\ny\nz"));
    }

    @Test
    public void testLineComment() {
        expect("foo%123\nbar", isAtom("foo"), isAtom("bar"));
        expect("%123\no", isAtom("o"));
    }

    @Test
    public void testBlockComment() {
        expect("a /*123*/  b", isAtom("a"), isAtom("b"));
        expect("foo/*123\n456*/bar", isAtom("foo"), isAtom("bar"));
        expect("foo\n/*\n456\n*/\nbar", isAtom("foo"), isAtom("bar"));
    }

    @Test
    public void testQuotedBrackets() {
        // From sec816.pl: test_val(atom_chars('[]', M), M, ['[', ']']),
        // This was failing - not surprisingly
        expect("['[',']']",
                isAtom("["),
                isAtom("["),
                isAtom(","),
                isAtom("]"),
                isAtom("]"));
    }

    @Test
    public void testQuotedCharList() {
        expect("`123` `abc`",
                isList(isAtom("1"), isAtom("2"), isAtom("3")),
                isList(isAtom("a"), isAtom("b"), isAtom("c")));
        expect("```` `\\`` `\\x41\\`",
                isList(isAtom("`")),
                isList(isAtom("`")),
                isList(isAtom("A")));
    }

    @Test
    public void testCharTranslation() {
        environment.getFlags().charConversion = true;
        environment.getCharConverter().add('f', 'F');
        environment.getCharConverter().add('x', '.');
        expect("foox", isUnboundVariable("Foo"), isAtom("."));
        environment.getFlags().charConversion = false;
        expect("foox", isAtom("foox"));
        // This one is interesting, as it's not documented how this should be handled
        environment.getCharConverter().add('#', '\'');
        environment.getFlags().charConversion = true;
        expect("foox #foox#' foox",
                isUnboundVariable("Foo"),
                isAtom("."),
                isAtom("foox#"), // first '#' is translated, second '#' is not
                isUnboundVariable("Foo"),
                isAtom("."));
    }

    @Test
    public void testBackQuoteToString() {
        readOptions.backquotedString = true;
        expect("`123` `abc`",
                isString("123"),
                isString("abc"));
    }

    @Test
    public void testDoubleQuotedModes() {
        readOptions.doubleQuotes = PrologFlags.Quotes.ATOM_symbol_char;
        expect("\"123\" \"abc\"",
                isList(isAtom("1"), isAtom("2"), isAtom("3")),
                isList(isAtom("a"), isAtom("b"), isAtom("c")));
        readOptions.doubleQuotes = PrologFlags.Quotes.ATOM_chars;
        expect("\"123\" \"abc\"",
                isList(isAtom("1"), isAtom("2"), isAtom("3")),
                isList(isAtom("a"), isAtom("b"), isAtom("c")));
        readOptions.doubleQuotes = PrologFlags.Quotes.ATOM_codes;
        expect("\"123\" \"abc\"",
                isList(isInteger('1'), isInteger('2'), isInteger('3')),
                isList(isInteger('a'), isInteger('b'), isInteger('c')));
        readOptions.doubleQuotes = PrologFlags.Quotes.ATOM_atom;
        expect("\"123\" \"abc\"",
                isAtom("123"),
                isAtom("abc"));
        readOptions.doubleQuotes = PrologFlags.Quotes.ATOM_string;
        expect("\"123\" \"abc\"",
                isString("123"),
                isString("abc"));
    }

    @Test
    public void testNoEscape() {
        readOptions.characterEscapes = false;
        expect("```` `\\`` `\\x41\\`",
                isList(isAtom("`")),
                isList(isAtom("\\"), isAtom("`")),
                isList(isAtom("\\"), isAtom("x"), isAtom("4"), isAtom("1"), isAtom("\\")));
    }

}
