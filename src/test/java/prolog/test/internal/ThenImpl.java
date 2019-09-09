// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.test.internal;

import org.hamcrest.Matcher;
import prolog.bootstrap.Interned;
import prolog.execution.Environment;
import prolog.execution.ExecutionState;
import prolog.execution.LocalContext;
import prolog.execution.Query;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.flags.ReadOptions;
import prolog.io.LogicalStream;
import prolog.library.Dictionary;
import prolog.test.StreamUtils;
import prolog.test.Then;
import prolog.variables.BoundVariable;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * PrologTest then clause. This clause allows testing results of a directive.
 */
public class ThenImpl implements Then {

    private final StateImpl state;
    private Query query;
    private ExecutionState lastExec = null;
    private Map<String, BoundVariable> vars = Collections.emptyMap();

    @Override
    public Term getVariableValue(String name) {
        BoundVariable var = vars.get(name);
        LocalContext throwAway = state.environment().newLocalContext();
        if (var == null) {
            var = new BoundVariable(throwAway, name, 0);
        }
        return var.resolve(throwAway).value(state.environment()); // make sure all recursive variables are resolved.
    }

    ThenImpl(StateImpl state) {
        this.state = state;
        query = new TestQuery(state.environment);
    }

    /**
     * Parse text for purpose of testing. This behaves similar to consult/query.
     *
     * @param text Text to interpret
     */
    void parse(String text) {
        try(ThenScope scope = new ThenScope(this)) {
            state.reset();
            query.reset();
            lastExec = null;
            LogicalStream stream = StreamUtils.logical(StreamUtils.prologString(text));
            Term term = stream.read(state.environment, null, new ReadOptions(state.environment, null));
            if (CompoundTerm.termIsA(term, Interned.QUERY_FUNCTOR, 1)) {
                CompoundTerm clause = (CompoundTerm) term;
                query.compile(clause.get(0));
                query.reset();
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
                assertTrue("Log is not empty", state.log.isEmpty());
                return this;
            }
            for (Matcher<? super Term> m : matchers) {
                if (m == null) {
                    assertTrue("Log is not empty", state.log.isEmpty());
                    continue;
                }
                assertFalse("Log is empty", state.log.isEmpty());
                Term t = state.log.pop();
                match(m, t);
            }
            return this;
        }
    }

    @Override
    public Then assertSuccess() {
        assertTrue("directive failed", lastExec == ExecutionState.SUCCESS);
        return this;
    }

    @Override
    public Then assertFailed() {
        assertTrue("directive succeeded", lastExec == ExecutionState.FAILED);
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
    public Then andWhen(Consumer<Then> lambda) {
        ThenImpl next = new ThenImpl(state);
        lambda.accept(next);
        return next;
    }

    @Override
    public Then andWhen(String text) {
        ThenImpl next = new ThenImpl(state);
        next.parse(text);
        return next;
    }

    @Override
    public Then anotherSolution() {
        state.environment().backtrack();
        cycle();
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
