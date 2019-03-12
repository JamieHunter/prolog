// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.test.internal;

import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.predicates.BuiltinPredicateArity0;
import prolog.predicates.BuiltinPredicateArity1;

import java.util.LinkedList;

/**
 * PrologTest state, with modified environment to observe results of executing directives.
 */
public class StateImpl {

    final TestEnvironment environment = new TestEnvironment();
    int callDepth;
    int backtrackDepth;
    final LinkedList<Term> log = new LinkedList<>();

    StateImpl() {
        reset();
    }

    /**
     * Special subclass of environment capturing various hooks.
     */
    private class TestEnvironment extends Environment {

        TestEnvironment() {
            // The ##call_depth predicate captures call stack depth
            setBuiltinPredicate(getAtom("##call_depth"), 0,
                    new BuiltinPredicateArity0(e -> callDepth = e.getCallStackDepth()));
            // The ##backtrack_depth predicate captures backtrack stack depth
            setBuiltinPredicate(getAtom("##backtrack_depth"), 0,
                    new BuiltinPredicateArity0(e -> backtrackDepth = e.getBacktrackDepth()));
            // The ##expectLog is a great way of persisting history without writing to file
            setBuiltinPredicate(getAtom("##expectLog"), 1,
                    new BuiltinPredicateArity1((e, t) -> log.add(t)));
        }
    }


    Environment environment() {
        return environment;
    }

    void reset() {
        callDepth = -1;
        backtrackDepth = -1;
        log.clear();
    }

}
