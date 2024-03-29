package org.jprolog.flags;

import org.jprolog.library.OptionTest;
import org.junit.jupiter.api.Test;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.exceptions.FutureFlagValueError;
import org.jprolog.exceptions.FutureTypeError;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.Term;
import org.jprolog.io.PrologInputStream;
import org.jprolog.library.Io;
import org.jprolog.parser.ExpressionReader;
import org.jprolog.parser.Tokenizer;
import org.jprolog.test.StreamUtils;
import org.jprolog.variables.Variable;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.jprolog.test.Matchers.isAtom;

/**
 * Test the built-in processing of flag/option parameters. Note that there should be parity between this and
 * option.pl {@link OptionTest}
 */
public class OptionParserTest implements Flags {

    private final OptionParser<OptionParserTest> parser = new OptionParser<>();
    private final Environment environment = new Environment();
    private Boolean bool_flag = null;
    private PrologAtomLike atom_flag = null;
    private Long int_flag = null;
    private SampleEnum enum_flag = null;
    private Set<SampleEnum> enum_set = null;
    private Term term_flag = null;

    enum SampleEnum {
        ATOM_one,
        ATOM_two,
        ATOM_three
    }

    {
        // option that takes true/false
        parser.booleanFlag(environment.internAtom("bool_flag"), (o, v) -> o.bool_flag = v);
        parser.atomFlag(environment.internAtom("atom_flag"), (o, v) -> o.atom_flag = v);
        parser.intFlag(environment.internAtom("int_flag"), (o, v) -> o.int_flag = v);
        parser.enumFlag(environment.internAtom("enum_flag"), SampleEnum.class, (o, v) -> o.enum_flag = v);
        parser.enumFlags(environment.internAtom("enum_set"), SampleEnum.class, (o, v) -> o.enum_set = v);
        parser.other(environment.internAtom("term_flag"), (o, v) -> o.term_flag = v);
    }

    private void apply(String text) {
        PrologInputStream stream = StreamUtils.bufferedString(text);
        Tokenizer tok = new Tokenizer(environment, new ReadOptions(environment, null), stream);
        ExpressionReader reader = new ExpressionReader(tok);
        Term term = reader.read();
        assertThat(term, is(instanceOf(CompoundTerm.class)));
        Term end = reader.read();
        assertThat(end, is(Io.END_OF_FILE));

        parser.apply(environment, this, term);
    }

    @Test
    public void testBooleanTrue() {
        apply("[bool_flag(true)].");
        assertThat(bool_flag, is(true));
    }

    @Test
    public void testBooleanFalse() {
        apply("[bool_flag(false)].");
        assertThat(bool_flag, is(false));
    }

    @Test
    public void testBooleanEqTrue() {
        apply("[bool_flag = true].");
        assertThat(bool_flag, is(true));
    }

    @Test
    public void testBooleanEqFalse() {
        apply("[bool_flag = false].");
        assertThat(bool_flag, is(false));
    }

    @Test
    public void testBooleanOther() {
        assertThrows(FutureFlagValueError.class, () -> {
            apply("[bool_flag(other)].");
        });
    }

    @Test
    public void testAtomAny() {
        apply("[atom_flag = any].");
        assertThat(atom_flag, isAtom("any"));
    }

    @Test
    public void testAtomFlagNotAtom() {
        assertThrows(FutureTypeError.class, () -> {
            apply("[atom_flag = X].");
        });
    }

    @Test
    public void testIntFlag() {
        apply("[int_flag(20)].");
        assertThat(int_flag, is(20L));
    }

    @Test
    public void testEnumFlag() {
        apply("[enum_flag(one)].");
        assertThat(enum_flag, is(SampleEnum.ATOM_one));
        apply("[enum_flag(two)].");
        assertThat(enum_flag, is(SampleEnum.ATOM_two));
        apply("[enum_flag(three)].");
        assertThat(enum_flag, is(SampleEnum.ATOM_three));
    }

    @Test
    public void testEnumBadValue() {
        assertThrows(FutureFlagValueError.class, () -> {
            apply("[enum_flag(four)].");
        });
    }

    @Test
    public void testEnumSet() {
        apply("[enum_set = [one, two]].");
        assertThat(enum_set, is(instanceOf(Set.class)));
        assertThat(enum_set, containsInAnyOrder(SampleEnum.ATOM_one, SampleEnum.ATOM_two));
    }

    @Test
    public void testOtherFlag() {
        apply("[term_flag = X].");
        assertThat(term_flag, is(instanceOf(Variable.class)));
    }
}
