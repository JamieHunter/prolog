package prolog.io;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import prolog.bootstrap.Interned;
import prolog.constants.PrologAtomInterned;
import prolog.constants.PrologEmptyList;
import prolog.execution.Environment;
import prolog.execution.OperatorEntry;
import prolog.expressions.Term;
import prolog.expressions.TermListImpl;
import prolog.flags.ReadOptions;
import prolog.flags.WriteOptions;
import prolog.parser.ExpressionReader;
import prolog.parser.Tokenizer;
import prolog.test.StreamUtils;

import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToCompressingWhiteSpace;
import static org.junit.Assert.assertThat;

/**
 * Test conversion to strings
 */
public class StructureWriterTest {

    private Environment environment;
    private WriteOptions options;

    @BeforeEach
    public void setEnvironment() {
        environment = new Environment();
        options = new WriteOptions(environment, null);
        options.quoted = true; // by default, auto-quote on
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
        String text = StructureWriter.toString(environment, source, options);
        assertThat(text, matcher);
    }

    @Test
    public void testSimpleAtom() throws IOException {
        options.quoted = true; // assume quoting has been enabled
        expect(Interned.TRUE_ATOM, equalTo("true"));
        expect(Interned.COMMA_FUNCTOR, equalTo(","));
        expect(Interned.CLAUSE_FUNCTOR, equalTo(":-"));
    }

    @Test
    public void testAutoquoteAtom() throws IOException {
        options.quoted = true; // assume quoting has been enabled
        expect(atom(",,"), equalTo("',,'"));
        expect(atom("123"), equalTo("'123'"));
        expect(atom("Abc"), equalTo("'Abc'"));
        expect(atom("abc\ndef"), equalTo("'abc\\ndef'"));
        expect(atom("abc\016def"), equalTo("'abc\\016\\def'"));
    }

    @Test
    public void testNoAutoquoteAtom() throws IOException {
        options.quoted = false; // quoting is disabled
        expect(atom(",,"), equalTo(",,"));
        expect(atom("123"), equalTo("123"));
        expect(atom("Abc"), equalTo("Abc"));
        expect(atom("abc\ndef"), equalTo("abc\ndef"));
        expect(atom("abc\016def"), equalTo("abc\016def"));
        expect(Interned.TRUE_ATOM, equalTo("true"));
        expect(Interned.COMMA_FUNCTOR, equalTo(","));
        expect(Interned.CLAUSE_FUNCTOR, equalTo(":-"));
    }

    @Test
    public void testList() throws IOException {
        options.ignoreOps = false;
        expect(new TermListImpl(new Term[]{atom("aa"), atom("bb")}, PrologEmptyList.EMPTY_LIST), equalTo("[aa, bb]"));
        expect(new TermListImpl(new Term[]{atom("a"), atom("b")}, PrologEmptyList.EMPTY_LIST), equalTo("`ab`"));
        expect(new TermListImpl(new Term[]{atom("aa"), atom("bb")}, atom("cc")), equalTo("[aa, bb| cc]"));
        options.ignoreOps = true;
        expect(new TermListImpl(new Term[]{atom("aa"), atom("bb")}, PrologEmptyList.EMPTY_LIST), equalTo(".(aa, .(bb,[]))"));
        expect(new TermListImpl(new Term[]{atom("a"), atom("b")}, PrologEmptyList.EMPTY_LIST), equalTo(".(a, .(b,[]))"));
        expect(new TermListImpl(new Term[]{atom("aa"), atom("bb")}, atom("cc")), equalTo(".(aa, .(bb,cc))"));
    }

    @Test
    public void testExpression() throws IOException {
        options.ignoreOps = false;
        expect(read("1 + 2 * 3."), equalToCompressingWhiteSpace("1+2*3"));
        expect(read("+(1, *(2, 3))."), equalToCompressingWhiteSpace("1+2*3"));
        expect(read("1 * 2 + 3."), equalToCompressingWhiteSpace("1*2+3"));
        expect(read("1 * (2 + 3)."), equalToCompressingWhiteSpace("1*(2+3)"));
        expect(read("(1 + 2) * (3 + 4)."), equalToCompressingWhiteSpace("(1+2)*(3+4)"));
        expect(read("(1 * 2) + (3 * 4)."), equalToCompressingWhiteSpace("1*2+3*4"));
        expect(read("1 * 2 * 3 * 4."), equalToCompressingWhiteSpace("1*2*3*4"));
        expect(read("1 * (2 * 3) * 4."), equalToCompressingWhiteSpace("1*(2*3)*4"));
        expect(read("(1 =:= 2) =:= (3 =:= 4)."), equalToCompressingWhiteSpace("(1=:=2)=:=(3=:=4)"));
        options.ignoreOps = true;
        expect(read("1 + 2 * 3."), equalToCompressingWhiteSpace("+(1, *(2,3))"));
        expect(read("+(1, *(2,3))."), equalToCompressingWhiteSpace("+(1, *(2,3))"));
    }

    @Test
    public void testCommaHandling() throws IOException {
        options.ignoreOps = false;
        expect(read("a(1,(2,3))."), equalToCompressingWhiteSpace("a(1,(2,3))"));
        options.ignoreOps = true;
        expect(read("a(1,(2,3))."), equalToCompressingWhiteSpace("a(1,(,(2,3)))"));
    }

    @Test
    public void testUnaryPrefixHandling() throws IOException {
        expect(read("-(1^2)."), equalToCompressingWhiteSpace("-(1^2)"));
        expect(read("?(1^2)."), equalToCompressingWhiteSpace("?1^2"));
    }

    @Test
    public void testUnaryPostfixHandling() throws IOException {
        expect(read("(1;2)@@@ ."), equalToCompressingWhiteSpace("(1;2)@@@"));
        expect(read("(1^2)@@@ ."), equalToCompressingWhiteSpace("1^2@@@"));
    }

}
