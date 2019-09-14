package org.jprolog.parser;

import org.jprolog.test.Matchers;
import org.jprolog.test.PrologTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test ability to discover variables during read process
 */
public class ReaderVarsTest {

    @Test
    public void testDiscoverVariables() {
        PrologTest.given().when("?- read_term_from_atom('term(P,A,_,T,X,T)', Q, [variables(V), variable_names(VN)]).")
                .assertSuccess()
                .variable("Q", Matchers.isCompoundTerm("term",
                        Matchers.isVariable("P"),
                        Matchers.isVariable("A"),
                        Matchers.isUninstantiated(),
                        Matchers.isVariable("T"),
                        Matchers.isVariable("X"),
                        Matchers.isVariable("T")))
                .variable("V", Matchers.isList(
                        Matchers.isVariable("P"),
                        Matchers.isVariable("A"),
                        Matchers.isVariable("T"),
                        Matchers.isVariable("X")))
                .variable("VN", Matchers.isList(
                        Matchers.isCompoundTerm("=", Matchers.isAtom("P"), Matchers.isVariable("P")),
                        Matchers.isCompoundTerm("=", Matchers.isAtom("A"), Matchers.isVariable("A")),
                        Matchers.isCompoundTerm("=", Matchers.isAtom("T"), Matchers.isVariable("T")),
                        Matchers.isCompoundTerm("=", Matchers.isAtom("X"), Matchers.isVariable("X"))
                                ))
        ;
    }
}
