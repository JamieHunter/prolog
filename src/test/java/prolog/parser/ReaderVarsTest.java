package prolog.parser;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import prolog.exceptions.PrologSyntaxError;
import prolog.execution.Environment;
import prolog.execution.OperatorEntry;
import prolog.expressions.Term;
import prolog.flags.ReadOptions;
import prolog.io.PrologInputStream;
import prolog.library.Io;
import prolog.test.PrologTest;
import prolog.test.StreamUtils;
import prolog.variables.Variable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static prolog.test.Matchers.*;

/**
 * Test ability to discover variables during read process
 */
public class ReaderVarsTest {

    @Test
    public void testDiscoverVariables() {
        PrologTest.given().when("?- read_term_from_atom('term(P,A,_,T,X,T)', Q, [variables(V), variable_names(VN)]).")
                .assertSuccess()
                .variable("Q", isCompoundTerm("term",
                        isVariable("P"),
                        isVariable("A"),
                        isUninstantiated(),
                        isVariable("T"),
                        isVariable("X"),
                        isVariable("T")))
                .variable("V", isList(
                        isVariable("P"),
                        isVariable("A"),
                        isVariable("T"),
                        isVariable("X")))
                .variable("VN", isList(
                        isCompoundTerm("=", isAtom("P"), isVariable("P")),
                        isCompoundTerm("=", isAtom("A"), isVariable("A")),
                        isCompoundTerm("=", isAtom("T"), isVariable("T")),
                        isCompoundTerm("=", isAtom("X"), isVariable("X"))
                                ))
        ;
    }
}
