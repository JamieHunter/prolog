// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.test.internal;

import org.jprolog.constants.PrologAtomInterned;
import org.jprolog.constants.PrologInteger;
import org.jprolog.exceptions.PrologDomainError;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.Term;
import org.jprolog.expressions.TermList;
import org.jprolog.flags.StreamProperties;
import org.jprolog.io.LogicalStream;
import org.jprolog.predicates.BuiltinPredicateArity0;
import org.jprolog.predicates.BuiltinPredicateArity1;
import org.jprolog.predicates.BuiltinPredicateArity2;
import org.jprolog.unification.Unifier;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * PrologTest state, with modified environment to observe results of executing directives.
 */
public class StateImpl {

    final TestEnvironment environment = new TestEnvironment();
    int callDepth;
    int backtrackDepth;
    final LinkedList<Term> log = new LinkedList<>();
    final HashMap<String, OutputMonitor> textLogs = new HashMap<>();

    StateImpl() {
        reset();
    }

    /**
     * Special subclass of environment capturing various hooks.
     */
    private class TestEnvironment extends Environment {

        TestEnvironment() {
            // The ##call_depth predicate captures call stack depth
            setBuiltinPredicate(internAtom("##call_depth"), 0,
                    new BuiltinPredicateArity0(e -> callDepth = e.getCallStackDepth()));
            // The ##backtrack_depth predicate captures backtrack stack depth
            setBuiltinPredicate(internAtom("##backtrack_depth"), 0,
                    new BuiltinPredicateArity0(e -> backtrackDepth = e.getBacktrackDepth()));
            // The ##expectLog is a great way of persisting history without writing to file
            setBuiltinPredicate(internAtom("##expectLog"), 1,
                    new BuiltinPredicateArity1((e, t) -> log.add(t)));
            // The ##text_log creates a special text-log stream
            setBuiltinPredicate(internAtom("##text_log"), 2,
                    new BuiltinPredicateArity2(StateImpl.this::textLog));
            // the ##text_matches is the number of match counts for text_log
            setBuiltinPredicate(internAtom("##text_matches"), 2,
                    new BuiltinPredicateArity2(StateImpl.this::textMatches));
            // the ##set_stdout changes the stream for stdout
            setBuiltinPredicate(internAtom("##set_default_streams"), 0,
                    new BuiltinPredicateArity0(StateImpl.this::setDefaultStreams));
        }
    }

    private void textMatches(Environment environment, Term aliasTerm, Term unifyTerm) {
        PrologAtomInterned alias = PrologAtomInterned.from(environment, aliasTerm);
        OutputMonitor monitor = textLogs.get(alias.name());
        if (monitor == null) {
            throw PrologDomainError.error(environment, "##text_log", alias);
        }
        Term count = PrologInteger.from(monitor.getCount());
        if (!Unifier.unify(environment.getLocalContext(), unifyTerm, count)) {
            environment.backtrack();
        }
    }

    private void textLog(Environment environment, Term aliasTerm, Term grepTerm) {
        PrologAtomInterned alias = PrologAtomInterned.from(environment, aliasTerm);
        String grepString = TermList.extractString(environment, grepTerm);
        String aliasName = alias.name();
        OutputMonitor monitor = new OutputMonitor(grepString);
        PrologInteger id = LogicalStream.unique();
        LogicalStream binding = new LogicalStream(id, null, monitor, StreamProperties.OpenMode.ATOM_write);
        binding.setBufferMode(StreamProperties.Buffering.ATOM_line);
        binding.setType(StreamProperties.Type.ATOM_text);
        binding.setCloseOnAbort(true);
        binding.setEncoding(StreamProperties.Encoding.ATOM_utf8);
        binding.addAlias(alias);
        textLogs.put(aliasName, monitor);
        environment.addStream(id, binding);
        environment.addStreamAlias(alias, binding);
    }

    private void setDefaultStreams(Environment environment) {
        environment.setDefaultStreams();
    }

    public OutputMonitor getTextLog(String alias) {
        return textLogs.get(alias);
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
