package prolog.test;

import org.hamcrest.Matcher;
import prolog.expressions.Term;
import prolog.variables.Variable;

public interface Then {
    Then variable(String name, Matcher<? super Term> matcher);

    Then assertSuccess();
    Then assertFailed();

    Then callDepth(Matcher<? super Integer> m);
    Then backtrackDepth(Matcher<? super Integer> m);

    Then andWhen(String text);
}
