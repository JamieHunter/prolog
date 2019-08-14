package prolog.io;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import prolog.constants.PrologAtomInterned;
import prolog.constants.PrologEmptyList;
import prolog.execution.Environment;
import prolog.bootstrap.Interned;
import prolog.execution.OperatorEntry;
import prolog.expressions.Term;
import prolog.expressions.TermListImpl;
import prolog.flags.ReadOptions;
import prolog.parser.ExpressionReader;
import prolog.parser.Tokenizer;
import prolog.test.StreamUtils;

import java.io.IOException;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test conversion to strings
 */
public class StructureWriterTest {

    private Environment environment;
    @Before
    public void setEnvironment() {
        environment = new Environment();
        // fake postfix operator
        environment.makeOperator(450, OperatorEntry.Code.YF, environment.internAtom("@@@")); // between * and +
    }

    private PrologAtomInterned atom(String name) {
        return environment.internAtom(name);
    }

    private Term read(String text) {
        PrologInputStream stream = StreamUtils.bufferedString(text);
        Tokenizer tok = new Tokenizer(environment, new ReadOptions(environment, null), stream);
        return new ExpressionReader(tok).read();
    }

    private void expect(Term source, Matcher<? super String> matcher) {
        String text = StructureWriter.toString(environment, source);
        assertThat(text, matcher);
    }

    @Test
    public void testSimpleAtom() throws IOException {
        expect(Interned.TRUE_ATOM, equalTo("true"));
        expect(Interned.COMMA_FUNCTOR, equalTo(","));
        expect(Interned.CLAUSE_FUNCTOR, equalTo(":-"));
    }

    @Test
    public void testAutoquoteAtom() throws IOException {
        expect(atom(",,"), equalTo("',,'"));
        expect(atom("123"), equalTo("'123'"));
        expect(atom("Abc"), equalTo("'Abc'"));
        expect(atom("abc\ndef"), equalTo("'abc\\ndef'"));
        expect(atom("abc\016def"), equalTo("'abc\\016\\def'"));
    }

    @Test
    public void testList() throws IOException {
        expect(new TermListImpl(new Term[] { atom("aa"), atom("bb") }, PrologEmptyList.EMPTY_LIST), equalTo("[aa, bb]"));
        expect(new TermListImpl(new Term[] { atom("a"), atom("b") }, PrologEmptyList.EMPTY_LIST), equalTo("`ab`"));
        expect(new TermListImpl(new Term[] { atom("aa"), atom("bb") }, atom("cc")), equalTo("[aa, bb| cc]"));
    }

    @Test
    public void testExpression() throws IOException {
        expect(read("1 + 2 * 3."), equalToIgnoringWhiteSpace("1+2*3"));
        expect(read("1 * 2 + 3."), equalToIgnoringWhiteSpace("1*2+3"));
        expect(read("1 * (2 + 3)."), equalToIgnoringWhiteSpace("1*(2+3)"));
        expect(read("(1 + 2) * (3 + 4)."), equalToIgnoringWhiteSpace("(1+2)*(3+4)"));
        expect(read("(1 * 2) + (3 * 4)."), equalToIgnoringWhiteSpace("1*2+3*4"));
        expect(read("1 * 2 * 3 * 4."), equalToIgnoringWhiteSpace("1*2*3*4"));
        expect(read("1 * (2 * 3) * 4."), equalToIgnoringWhiteSpace("1*(2*3)*4"));
        expect(read("(1 =:= 2) =:= (3 =:= 4)."), equalToIgnoringWhiteSpace("(1=:=2)=:=(3=:=4)"));
    }

    @Test
    public void testCommaHandling() throws IOException {
        expect(read("a(1,(2,3))."), equalToIgnoringWhiteSpace("a(1, (2,3))"));
    }

    @Test
    public void testUnaryPrefixHandling() throws IOException {
        expect(read("-(1^2)."), equalToIgnoringWhiteSpace("-(1^2)"));
        expect(read("?(1^2)."), equalToIgnoringWhiteSpace("?1^2"));
    }

    @Test
    public void testUnaryPostfixHandling() throws IOException {
        expect(read("(1;2)@@@ ."), equalToIgnoringWhiteSpace("(1;2)@@@"));
        expect(read("(1^2)@@@ ."), equalToIgnoringWhiteSpace("1^2@@@"));
    }

}
