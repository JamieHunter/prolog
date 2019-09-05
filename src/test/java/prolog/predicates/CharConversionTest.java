package prolog.predicates;

import org.junit.Ignore;
import org.junit.Test;
import prolog.test.Given;
import prolog.test.PrologTest;

import static prolog.test.Matchers.*;

public class CharConversionTest {

    Given given() {
        return PrologTest.
                given("?- char_conversion(a, b).")
                .and("?- char_conversion(b, x).")
                ;
    }

    @Ignore("conversion not yet implemented")
    @Test
    public void testParserConversion() {
        // Actual parser conversion is not yet implemented,
        // but requires setting of the prolog flag
    }

    @Test
    public void testCharConversionScanSingle() {
        given()
                .when("?- current_char_conversion(a, b).")
                .assertSuccess()
                .andWhen("?- current_char_conversion(a, a).")
                .assertFailed()
                .andWhen("?- current_char_conversion(b, x).")
                .assertSuccess()
                .andWhen("?- current_char_conversion(x, x).")
                .assertSuccess()
                .anotherSolution()
                .assertFailed();

    }

    @Test
    public void testCharConversionScanSingleUnify() {
        given()
                .when("?- current_char_conversion(a, X).")
                .assertSuccess()
                .variable("X", isAtom("b"))
                .anotherSolution()
                .assertFailed();
    }

    @Test
    public void testCharConversionScanMultipleUnify() {
        given()
                .when("?- current_char_conversion(X, x).")
                .assertSuccess()
                .variable("X", isAtom("b"))
                .anotherSolution()
                .assertSuccess()
                .variable("X", isAtom("x"))
                .anotherSolution()
                .assertFailed();
    }
}
