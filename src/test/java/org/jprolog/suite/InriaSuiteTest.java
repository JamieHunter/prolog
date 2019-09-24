package org.jprolog.suite;

import org.jprolog.bootstrap.Interned;
import org.jprolog.constants.PrologAtom;
import org.jprolog.exceptions.PrologThrowable;
import org.jprolog.expressions.CompoundTermImpl;
import org.jprolog.expressions.Term;
import org.jprolog.expressions.TermList;
import org.jprolog.flags.WriteOptions;
import org.jprolog.io.StructureWriter;
import org.jprolog.test.Given;
import org.jprolog.test.Then;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

public class InriaSuiteTest extends Suite {

    static final int MAX_TIMES = 100;

    private Then loadInriaSuite() {
        return given(Paths.get("inriasuite"))
                .when("?- ['inriasuite.pl'].").assertSuccess();
    }

    @Disabled("INRIA Native test suite ignored, it will fail")
    @Test
    public void testInriaSuiteNative() {
        loadInriaSuite().andWhen("?- run_all_tests.").assertSuccess();
    }

    @TestFactory
    public List<DynamicTest> testInriaSuiteFactory() {
        Then loaded = loadInriaSuite();
        // collect the set of files
        ArrayList<DynamicTest> tests = new ArrayList<>();
        loaded = loaded.andWhen("?- file(N), absolute_file_name(N,P).");
        while (loaded.isSuccess()) {
            Term value = loaded.getVariableValue("P");
            String name = value.toString();
            Path path = Paths.get(name);
            tests.add(DynamicTest.dynamicTest("Inria test " + name,
                    path.toUri(), () -> testInriaFile(path)));
            loaded.anotherSolution();
        }
        return tests;
    }

    private void testInriaFile(Path path) {
        PrologAtom successAtom = new PrologAtom("success");
        PrologAtom failureAtom = new PrologAtom("failure");
        Then loaded = loadInriaSuite();
        Given chain = loaded.and();
        Term tests = chain
                .when("?- read_tests(\"" + path.toString().replace("\\", "\\\\") + "\", Tests).")
                .assertSuccess()
                .getVariableValue("Tests");
        for (Term test : TermList.extractList(tests)) {
            List<Term> parts = TermList.extractList(test);
            if (parts.size() != 2) {
                fail("Invalid test file, entry: " + tests);
            }
            Term goal = parts.get(0);
            Term expectations = parts.get(1);
            if (TermList.isList(expectations)) {
                testInriaGoalSolutions(chain, goal, TermList.extractList(expectations));
            } else if (successAtom.compareTo(expectations) == 0) {
                testInriaGoalSuccess(chain, goal);
            } else if (failureAtom.compareTo(expectations) == 0) {
                testInriaGoalExpectFailure(chain, goal);
            } else {
                // assume everything else is an exception?
                testInriaGoalExpectException(chain, goal, expectations);
            }
        }
    }

    private String formatTerm(Given given, Term goal) {
        StringBuilder builder = new StringBuilder();
        given.environment(e -> {
            WriteOptions opts = new WriteOptions(e, null);
            builder.append(StructureWriter.toString(e, goal, opts));
        });
        return builder.toString();
    }

    private Term prepareGoal(Term goal) {
        return new CompoundTermImpl(Interned.QUERY_FUNCTOR, goal);
    }

    private void testInriaGoalSuccess(Given given, Term goal) {
        Then result = given.when(prepareGoal(goal));
        if (result.isSuccess()) {
            result.anotherSolution();
            if (result.isSuccess()) {
                fail("Expected " + formatTerm(given, goal) + " to only succeed once, but it succeeded multiple times.");
            }
            return;
        }
        fail("Expected " + formatTerm(given, goal) + " to succeed, but it failed.");
    }

    private void testInriaGoalExpectFailure(Given given, Term goal) {
        Then result = given.when(prepareGoal(goal));
        if (!result.isSuccess()) {
            return;
        }
        fail("Expected " + formatTerm(given, goal) + " to fail, but it succeeded.");
    }

    private void testInriaGoalSolutions(Given given, Term goal, List<Term> solutions) {
        if (solutions.size() == 0) {
            testInriaGoalExpectFailure(given, goal);
            return;
        }
        Then result = given.when(prepareGoal(goal));
        int times;
        int expected = solutions.size();
        for (times = 0; times < expected; times++) {
            if (!result.isSuccess()) {
                if (times == 0) {
                    fail("Expected " + formatTerm(given, goal) + " to succeed " + expected + " times, but it failed.");
                } else {
                    fail("Expected " + formatTerm(given, goal) + " to succeed " + expected + " times, but it succeeded only " + times + " times.");
                }
                return;
            }
            // TODO: pattern match and remove a solution from set
            result.anotherSolution();
        }
        if (result.isSuccess()) {
            while (result.isSuccess() && times < MAX_TIMES) {
                times++;
                result.anotherSolution();
            }
            if (times >= MAX_TIMES) {
                fail("Expected " + formatTerm(given, goal) + " to succeed only " + expected + " times, but it succeeded at least " + times + " times.");
            } else {
                fail("Expected " + formatTerm(given, goal) + " to succeed only " + expected + " times, but it succeeded " + times + " times.");
            }
        }
    }

    private void testInriaGoalExpectException(Given given, Term goal, Term pattern) {
        try {
            Then result = given.when(prepareGoal(goal));
            if (result.isSuccess()) {
                fail("Expected " + formatTerm(given, goal) + " to throw " + pattern.toString() + " but instead goal succeeded.");
            } else {
                fail("Expected " + formatTerm(given, goal) + " to throw " + pattern.toString() + " but instead goal failed.");
            }
        } catch (PrologThrowable pe) {
            // TODO: pattern match error
        }
    }
}
