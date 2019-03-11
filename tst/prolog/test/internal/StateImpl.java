package prolog.test.internal;


import prolog.execution.Environment;
import prolog.bootstrap.Interned;
import prolog.execution.ExecutionState;
import prolog.execution.LocalContext;
import prolog.execution.Query;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.io.PrologReadStream;
import prolog.io.PrologReadStringStream;
import prolog.library.Dictionary;
import prolog.predicates.BuiltinPredicateArity0;
import prolog.variables.BoundVariable;

import java.util.Collections;
import java.util.Map;

/**
 * PrologTest state, with modified environment to observe results of executing directives.
 */
public class StateImpl {

    protected boolean succeeded = false;
    protected int callDepth = -1;
    protected int backtrackDepth = -1;
    protected Map<String, BoundVariable> vars = Collections.emptyMap();

    /**
     * Special subclass of environment capturing various hooks.
     */
    protected class TestEnvironment extends Environment {

        public TestEnvironment() {
            // The ##call_depth predicate captures call stack depth
            setBuiltinPredicate(getAtom("##call_depth"),0,
                    new BuiltinPredicateArity0(e-> callDepth = e.getCallStackDepth()));
            // The ##backtrack_depth predicate captures backtrack stack depth
            setBuiltinPredicate(getAtom("##backtrack_depth"),0,
                    new BuiltinPredicateArity0(e-> backtrackDepth = e.getBacktrackDepth()));
        }
    }

    protected class TestQuery extends Query {

        public TestQuery(Environment environment) {
            super(environment);
        }
    }

    protected final TestEnvironment environment = new TestEnvironment();

    public Environment environment() {
        return environment;
    }

    /**
     * Parse text for purpose of testing. This behaves similar to consult/query.
     * @param text Text to interpret
     * @return local context that was used for query
     */
    public LocalContext parse(String text) {
        Query query = new TestQuery(environment);
        PrologReadStream reader = new PrologReadStringStream(text);
        Term term = reader.read(environment);
        if (CompoundTerm.termIsA(term, Interned.QUERY_FUNCTOR, 1)) {
            CompoundTerm clause = (CompoundTerm) term;
            query.compile(clause.get(0));
            succeeded = query.run() == ExecutionState.SUCCESS;
            vars = query.getLocalContext().retrieveVariableMap();
        } else if (CompoundTerm.termIsA(term, Interned.CLAUSE_FUNCTOR, 1)) {
            // Directive, not implemented for tests at this time
            throw new UnsupportedOperationException("NYI");
        } else {
            Dictionary.addClauseZ(environment, term);
        }
        return query.getLocalContext();
    }

    public boolean succeeded() {
        return succeeded;
    }
    public int call_depth() {
        return callDepth;
    }
    public int backtrack_depth() {
        return backtrackDepth;
    }

}
