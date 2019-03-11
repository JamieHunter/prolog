package prolog.test;

import lombok.experimental.UtilityClass;
import prolog.expressions.Term;
import prolog.test.internal.GivenImpl;
import prolog.test.internal.StateImpl;

/**
 * All Prolog interpreter testing begins with this class.
 * Usually starting PrologTest.given()....
 */
@UtilityClass
public class PrologTest {
    public static Given given() {
        return new GivenImpl();
    }
    public static Given given(String text) {
        return given().that(text);
    }
}
