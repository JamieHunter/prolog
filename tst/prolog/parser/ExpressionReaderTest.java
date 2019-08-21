package prolog.parser;

import org.hamcrest.Matcher;
import org.junit.Test;
import prolog.execution.Environment;
import prolog.execution.OperatorEntry;
import prolog.expressions.Term;
import prolog.flags.ReadOptions;
import prolog.io.PrologInputStream;
import prolog.library.Io;
import prolog.test.StreamUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static prolog.test.Matchers.*;

/**
 * General parsing tests, some of these overlap in coverage with the Tokenizer test
 */
public class ExpressionReaderTest {

    // Note that generally, text should end with "."
    @SuppressWarnings("unchecked")
    private void expect(String text, Matcher<? super Term>... terms) {
        Environment environment = new Environment();
        // fake postfix operator
        environment.makeOperator(450, OperatorEntry.Code.YF, environment.internAtom("@@@")); // between * and +
        PrologInputStream stream = StreamUtils.bufferedString(text);
        Tokenizer tok = new Tokenizer(environment, new ReadOptions(environment, null), stream);
        ExpressionReader reader = new ExpressionReader(tok);

        for (int i = 0; i < terms.length; i++) {
            Term t = reader.read();
            assertThat(t, terms[i]);
        }
        Term t = reader.read();
        assertThat(t, is(Io.END_OF_FILE));
    }

    @Test
    public void testAtoms() {
        expect("abcd.", isAtom("abcd"));
        expect(">> .", isAtom(">>"));
        expect("'A>>\\'' .", isAtom("A>>'"));
    }


    @Test
    public void testPrefix() {
        expect("?- nl .", isCompoundTerm("?-", isAtom("nl")));
    }

    @Test
    public void testLeftAssocInfix() {
        expect("a * b / c .",
                isCompoundTerm("/",
                        isCompoundTerm("*", isAtom("a"), isAtom("b")),
                        isAtom("c")));
    }

    @Test
    public void testRightAssocInfix() {
        expect("a ^ b ^ c.",
                isCompoundTerm("^",
                        isAtom("a"),
                        isCompoundTerm("^",
                                isAtom("b"),
                                isAtom("c"))));
    }

    @Test
    public void testPrefixInfixLeft() {
        expect("-a+b.",
                isCompoundTerm("+",
                        isCompoundTerm("-", isAtom("a")),
                        isAtom("b")));
    }

    @Test
    public void testPostfixInfix() {
        expect("a+b@@@ .",
                isCompoundTerm("+",
                        isAtom("a"),
                        isCompoundTerm("@@@", isAtom("b"))));
    }

    @Test
    public void testNonTrivialPostfixInfix() {
        // '+'('a', '@@@'('*'( '@@@'('b'), 'c')))
        expect("a + b @@@ * c @@@ .",
                isCompoundTerm("+",
                        isAtom("a"),
                        isCompoundTerm("@@@",
                                isCompoundTerm("*",
                                        isCompoundTerm("@@@",
                                                isAtom("b")),
                                        isAtom("c")))));
    }

    @Test
    public void testExpression() {
        expect("a + (b - c).",
                isCompoundTerm("+",
                        isAtom("a"),
                        isCompoundTerm("-",
                                isAtom("b"),
                                isAtom("c"))));
    }

    @Test
    public void testCompoundTerm() {
        expect("likes(mary, chocolate).",
                isCompoundTerm("likes", isAtom("mary"), isAtom("chocolate")));
    }

    @Test
    public void testCommaQuirk() {
        // This test shows why it is necessary to use an intermediate representation of a bracketed expression
        expect("a(b,c,(d,e)).",
                isCompoundTerm("a", isAtom("b"), isAtom("c"),
                        isCompoundTerm(",", isAtom("d"), isAtom("e"))));
    }

    @Test
    public void testNamedVariable() {
        expect("Abcd.", isUnboundVariable("Abcd"));
    }

    @Test
    public void testAnonymousVariable() {
        expect("_.", isAnonymousVariable());
    }

    @Test
    public void testNegation() {
        expect("-123.", isInteger(-123));
    }

    @Test
    public void testNotNegation() {
        expect("-(123).", isCompoundTerm("-", isInteger(123)));
    }

    @Test
    public void testMixedCompoundsAndOperators() {

        // Test came from an actual parse bug
        // ':-' is lower than ',' and is also a prefix
        // This parses in a non-obvious way
        expect("'$x'(:- A, B) :- C.",
                isCompoundTerm(":-",
                        isCompoundTerm("$x",
                                isCompoundTerm(":-",
                                        isCompoundTerm(",",
                                                isUnboundVariable("A"),
                                                isUnboundVariable("B")))),
                        isUnboundVariable("C")));

        // This addresses the above parsing, but still treats ':-' as unary
        expect("'$x'((':-'(A)), B) :- C.",
                isCompoundTerm(":-",
                        isCompoundTerm("$x",
                                isCompoundTerm(":-",
                                        isUnboundVariable("A")),
                                isUnboundVariable("B")),
                        isUnboundVariable("C")));

        // This addresses the above parsing, using brackets. I don't see this in ISO
        // standard, but it's logically acceptable and not contrary to standard
        expect("'$x'((:-)(A), B) :- C.",
                isCompoundTerm(":-",
                        isCompoundTerm("$x",
                                isCompoundTerm(":-",
                                        isUnboundVariable("A")),
                                isUnboundVariable("B")),
                        isUnboundVariable("C")));

    }

    @Test
    public void testMixedOperator() {
        // This is an expression that failed to parse in sec816.pl
        expect("[-, '2','5'].",
                isList(isAtom("-"), isAtom("2"), isAtom("5")));
    }

    @Test
    public void testQuotedBrackets() {
        // From sec816.pl: test_val(atom_chars('[]', M), M, ['[', ']']),
        // This was failing
        expect("['[',']'].",
                isList(
                        isAtom("["),
                        isAtom("]")));
        expect("foo(['[',']']).",
                isCompoundTerm(
                        "foo",
                        isList(
                                isAtom("["),
                                isAtom("]"))));
    }
}
