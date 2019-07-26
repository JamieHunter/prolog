package prolog.parser;

import org.hamcrest.Matcher;
import org.junit.Test;
import prolog.constants.PrologEOF;
import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.flags.ReadOptions;
import prolog.io.PrologInputStream;
import prolog.test.StreamUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static prolog.test.Matchers.*;

/**
 * Test the tokenizer, which is responsible for parsing the minimum Term without interpretation.
 */
@SuppressWarnings("OctalInteger")
public class TokenizerTest {

    private void expect(String text, Matcher<? super Term>... terms) {
        Environment environment = new Environment();
        PrologInputStream stream = StreamUtils.bufferedString(text);
        Tokenizer tok = new Tokenizer(
                environment,
                new ReadOptions(environment, null),
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
                isAnonymousVariable());
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
        expect("_12.3", isAnonymousVariable(), isFloat(12.3));
        expect("12._3", isInteger(12), isAtom("."), isAnonymousVariable(), isInteger(3));
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
}
