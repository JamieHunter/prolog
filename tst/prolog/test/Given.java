package prolog.test;

public interface Given {
    Given that(String text);
    Given and(String text);
    Then when(String text);
}
