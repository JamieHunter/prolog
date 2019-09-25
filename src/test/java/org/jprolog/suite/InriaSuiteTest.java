package org.jprolog.suite;

import org.jprolog.bootstrap.Interned;
import org.jprolog.constants.PrologAtom;
import org.jprolog.constants.PrologAtomInterned;
import org.jprolog.enumerators.VariableCollector;
import org.jprolog.exceptions.PrologThrowable;
import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.CompoundTermImpl;
import org.jprolog.expressions.Term;
import org.jprolog.expressions.TermList;
import org.jprolog.flags.WriteOptions;
import org.jprolog.io.StructureWriter;
import org.jprolog.test.Given;
import org.jprolog.test.Then;
import org.jprolog.unification.Unifier;
import org.jprolog.variables.LabeledVariable;
import org.jprolog.variables.Variable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Generate a dynamic test per whitelisted file.
     * @return list of tests
     */
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

    /**
     * Test single test (dynamic)
     * @param path Path to Inria test file
     */
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
                assertAllSolutions(chain, goal, expectations);
            } else if (successAtom.compareTo(expectations) == 0) {
                assertSuccess(chain, goal);
            } else if (failureAtom.compareTo(expectations) == 0) {
                assertFailed(chain, goal);
            } else {
                // assume everything else is an exception?
                assertPrologException(chain, goal, expectations);
            }
        }
    }

    /**
     * Pretty-print a term
     * @param given Given context
     * @param term Term to format
     * @return
     */
    private String formatTerm(Given given, Term term) {
        Environment e = given.environment();
        WriteOptions opts = new WriteOptions(e, null);
        opts.quoted = true;
        return StructureWriter.toString(e, term, opts);
    }

    /**
     * Wraps a goal in ?-/1
     * @param goal
     * @return prepared goal
     */
    private Term prepareGoal(Term goal) {
        return new CompoundTermImpl(Interned.QUERY_FUNCTOR, goal);
    }

    /**
     * Assert that goal succeeded
     * @param given Given context
     * @param goal Goal under test
     */
    private void assertSuccess(Given given, Term goal) {
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

    /**
     * Assert goal failed
     * @param given Given context
     * @param goal Goal under test
     */
    private void assertFailed(Given given, Term goal) {
        Then result = given.when(prepareGoal(goal));
        if (!result.isSuccess()) {
            return;
        }
        fail("Expected " + formatTerm(given, goal) + " to fail, but it succeeded.");
    }

    /**
     * Assert that prolog throws an exception
     * @param given Given context
     * @param goal Goal under test
     * @param pattern Exception pattern
     */
    private void assertPrologException(Given given, Term goal, Term pattern) {
        try {
            Then result = given.when(prepareGoal(goal));
            if (result.isSuccess()) {
                fail("Expected " + formatTerm(given, goal) + " to throw " + formatTerm(given, pattern) + " but instead goal succeeded.");
            } else {
                fail("Expected " + formatTerm(given, goal) + " to throw " + formatTerm(given, pattern) + " but instead goal failed.");
            }
        } catch (PrologThrowable pe) {
            Term t = pe.value();
            if (CompoundTerm.termIsA(t, Interned.ERROR_FUNCTOR, 2)) {
                t = ((CompoundTerm) t).get(0); // formal error
            } else {
                // anything else thrown
                t = new CompoundTermImpl(new PrologAtom("unexpected_ball"), t);
            }
            Environment e = given.environment();
            LocalContext lc = e.newLocalContext();
            e.setLocalContext(lc);
            if (!Unifier.unify(lc, pattern, t)) {
                fail("Expected error signature " + formatTerm(given, pattern) + " but got " + formatTerm(given, t));
            }
        }
    }

    /**
     * Exhaust all possible solutions
     * @param given Given context
     * @param goal Goal under test
     * @param expectations Collection of expectations
     */
    private void assertAllSolutions(Given given, Term goal, Term expectations) {
        List<Term> solutions = TermList.extractList(expectations);
        if (solutions.size() == 0) {
            assertFailed(given, goal);
            return;
        }
        ArrayList<Term> extra = new ArrayList<>();
        // Before executing goal, analyze all possible variables
        VariableCollector collector = new VariableCollector(given.environment(), VariableCollector.Mode.COLLECT);
        goal.enumTerm(collector);
        List<? extends Variable> variables = collector.getVariables();
        // Execute goal and collect all solutions. Each solution is matched with the solution expectations.
        // Non-match goes into the "extra" list
        Then result = given.when(prepareGoal(goal));
        int times;
        int expected = solutions.size();
        for (times = 0; times < MAX_TIMES; times++) {
            if (!result.isSuccess()) {
                break;
            }
            extractOneSolution(given, result, goal, solutions, extra, variables);
            result.anotherSolution();
        }
        // Determine how to report mismatches
        StringBuilder builder = new StringBuilder();
        builder.append("Executed [" + formatTerm(given, goal) + "] " + times + " out of " + expected + " times.");
        if (solutions.size() > 0) {
            builder.append(" Missing solutions: ");
            for(Term soln : solutions) {
                builder.append(formatTerm(given, soln));
                builder.append(" ");
            }
        }
        if (extra.size() > 0) {
            builder.append(" Extra solutions: ");
            for(Term soln : extra) {
                builder.append(formatTerm(given, soln));
                builder.append(" ");
            }
        }
        if (times != times || solutions.size() > 0 || extra.size() > 0) {
            fail(builder.toString());
        }
    }

    /**
     * Either match/extract a solution, or place unique solution into extra list
     * @param given Given context
     * @param result Result context
     * @param goal Goal under test
     * @param solutions Remaining unmatched solution expectations
     * @param extra Extra solutions collected
     * @param variables Variables in goal
     */
    private void extractOneSolution(Given given, Then result, Term goal, List<Term> solutions, List<Term> extra, List<? extends Variable> variables) {
        for (int i = 0; i < solutions.size(); i++) {
            // attempt to satisfy solution constraints
            if (constraintsMatch(given, solutions.get(i))) {
                solutions.remove(i);
                return;
            }
        }
        extra.add(buildExtraSolution(given, result, variables));
    }

    /**
     * true if all constraints for the given solutions expectation are fulfilled.
     * @param given Given context
     * @param constraints List of constraints for given solution expectation
     * @return true if fulfilled
     */
    private boolean constraintsMatch(Given given, Term constraints) {
        PrologAtomInterned arrow = given.environment().internAtom("<--");
        List<Term> constraintsList = TermList.extractList(constraints);
        Environment e = given.environment();
        LocalContext context = e.getLocalContext();
        int backtrack = e.getBacktrackDepth(); // cheating
        try {
            for (Term constraint : constraintsList) {
                Term bound = constraint.resolve(context); // bind to same variable context
                if (!CompoundTerm.termIsA(bound, arrow, 2)) {
                    fail("Expected constraint in form [V <-- term, ...] but got " + formatTerm(given, constraints));
                }
                CompoundTerm pair = (CompoundTerm) bound;
                Term var = pair.get(0);
                Term value = pair.get(1);
                if (!var.isInstantiated()) {
                    if (value.isInstantiated()) {
                        // if var is specified, it is expected to be instantiated unless explicit wildcard
                        return false;
                    }
                    continue;
                }
                if (!Unifier.unify(context, var, value)) {
                    return false;
                }
            }
        } finally {
            e.trimBacktrackStackToDepth(backtrack);
        }
        return true;
    }

    /**
     * If a solution was not matched, construct something useful to aid debugging
     * @param given Given context
     * @param result Result context
     * @param variables Variables to construct mismatch from
     * @return Mismatching term
     */
    private Term buildExtraSolution(Given given, Then result, List<? extends Variable> variables) {
        PrologAtomInterned arrow = given.environment().internAtom("<--");
        Environment e = given.environment();
        List<Term> terms = variables.stream().map(v -> {
            if (v.isInstantiated()) {
                return new CompoundTermImpl(arrow, v.label(), v.value());
            } else if (!"_".equals(v.name())) {
                return new CompoundTermImpl(arrow, v.label(), new LabeledVariable("_", -1));
            } else {
                return null;
            }
        }).filter(t -> t != null).collect(Collectors.toList());
        return TermList.from(terms);
    }
}
