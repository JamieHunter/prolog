// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.test.internal;

import org.hamcrest.Matcher;
import org.jprolog.bootstrap.Interned;
import org.jprolog.execution.Environment;
import org.jprolog.execution.ExecutionState;
import org.jprolog.execution.LocalContext;
import org.jprolog.execution.Query;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.Term;
import org.jprolog.flags.ReadOptions;
import org.jprolog.io.LogicalStream;
import org.jprolog.library.Dictionary;
import org.jprolog.test.Given;
import org.jprolog.test.StreamUtils;
import org.jprolog.test.Then;
import org.jprolog.variables.ActiveVariable;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PrologTest then clause. This clause allows testing results of a directive.
 */
public class ThenImpl implements Then {

    private final GivenImpl given;
    private final StateImpl state;
    private Query query;
    private ExecutionState lastExec = null;
    private Map<String, ActiveVariable> vars = Collections.emptyMap();

    @Override
    public Term getVariableValue(String name) {
        ActiveVariable var = vars.get(name);
        LocalContext throwAway = state.environment().newLocalContext();
        if (var == null) {
            var = new ActiveVariable(state.environment(), name, state.environment().nextVariableId());
        }
        return var.resolve(throwAway).value(); // make sure all recursive variables are resolved.
    }

    ThenImpl(GivenImpl given, StateImpl state) {
        this.given = given;
        this.state = state;
        query = new TestQuery(state.environment);
    }

    void run(String text) {
        LogicalStream stream = StreamUtils.logical(StreamUtils.prologString(text));
        Term term = stream.read(state.environment, null, new ReadOptions(state.environment, null));
        run(term);
    }

    void run(Term term) {
        try(ThenScope scope = new ThenScope(this)) {
            state.reset();
            query.reset();
            lastExec = null;
            if (CompoundTerm.termIsA(term, Interned.QUERY_FUNCTOR, 1)) {
                CompoundTerm clause = (CompoundTerm) term;
                query.prepare(clause.get(0));
                query.start();
                cycle();
            } else if (CompoundTerm.termIsA(term, Interned.CLAUSE_FUNCTOR, 1)) {
                // Directive, not implemented for tests at this time
                throw new UnsupportedOperationException("NYI");
            } else {
                Dictionary.addClauseZ(state.environment, term);
            }
        }
    }

    private void cycle() {
        lastExec = query.cycle();
        vars = query.getLocalContext().retrieveVariableMap();
    }

    @Override
    public Then variable(String name, Matcher<? super Term> matcher) {
        Term value = getVariableValue(name);
        try(ThenScope scope = new ThenScope(this)) {
            match(matcher, value);
            return this;
        }
    }

    @Override
    @SafeVarargs
    public final Then expectLog(Matcher<? super Term>... matchers) {
        try(ThenScope scope = new ThenScope(this)) {
            if (matchers == null || matchers.length == 0) {
                assertTrue(state.log.isEmpty(), "Log is not empty");
                return this;
            }
            for (Matcher<? super Term> m : matchers) {
                if (m == null) {
                    assertTrue(state.log.isEmpty(), "Log is not empty");
                    continue;
                }
                assertFalse(state.log.isEmpty(), "Log is empty");
                Term t = state.log.pop();
                match(m, t);
            }
            return this;
        }
    }

    @Override
    public boolean isSuccess() {
        return lastExec == ExecutionState.SUCCESS;
    }

    @Override
    public Then assertSuccess() {
        if (!isSuccess()) {
            fail("directive failed when it should have succeeded");
        }
        return this;
    }

    @Override
    public Then assertFailed() {
        if (lastExec != ExecutionState.FAILED) {
            fail("directive succeeded when it should have failed");
        }
        return this;
    }

    @Override
    public Then callDepth(Matcher<? super Integer> m) {
        try(ThenScope scope = new ThenScope(this)) {
            assertThat(state.callDepth, m);
            return this;
        }
    }

    @Override
    public Then backtrackDepth(Matcher<? super Integer> m) {
        try(ThenScope scope = new ThenScope(this)) {
            assertThat(state.backtrackDepth, m);
            return this;
        }
    }

    @Override
    public Given and() {
        return given;
    }

    @Override
    public Then andWhen(String text) {
        return and().when(text);
    }

    @Override
    public Then anotherSolution() {
        state.environment().backtrack();
        cycle();
        return this;
    }

    @Override
    public Then solutions(Consumer<Then> ... solutions) {
        if (solutions.length == 0) {
            // alias for assert Failed
            return assertFailed();
        }
        for(int soln = 0; soln < solutions.length; soln ++) {
            if(!isSuccess()) {
                if (soln == 0) {
                    fail("no solutions provided out of " + solutions.length + " expected");
                } else {
                    fail("only " + soln + " solution(s) provided out of " + solutions.length + " expected");
                }
            };
            solutions[soln].accept(this);
            anotherSolution();
        }
        if (isSuccess()) {
            // see how many additional solutions were provided
            int count = solutions.length;
            while(isSuccess()) {
                count++;
                anotherSolution();
            }
            if (solutions.length == 0) {
                fail(count + " solution(s) provided when none expected");
            } else {
                fail(count + " solution(s) provided when only " + solutions.length + " solution(s) expected");
            }
        };
        return this;
    }

    @Override
    public OutputMonitor getTextLog(String alias) {
        return state.getTextLog(alias);
    }

    @Override
    public Then textMatches(String alias, Matcher<Integer> matcher) {
        OutputMonitor monitor = getTextLog(alias);
        int count = monitor.getCount();
        assertThat(alias + " match count mismatch", count, matcher);
        return this;
    }

    private static void match(Matcher<? super Term> matcher, Term value) {
        assertThat(value, matcher);
    }


    private class TestQuery extends Query {

        TestQuery(Environment environment) {
            super(environment);
        }
    }

}
