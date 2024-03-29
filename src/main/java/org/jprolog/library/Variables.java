// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Interned;
import org.jprolog.bootstrap.Predicate;
import org.jprolog.constants.PrologInteger;
import org.jprolog.enumerators.VariableCollector;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.CompoundTermImpl;
import org.jprolog.expressions.Term;
import org.jprolog.expressions.TermList;
import org.jprolog.unification.Unifier;
import org.jprolog.variables.Variable;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Bootstraps variable analysis predicates.
 */
public class Variables {

    private Variables() {
    }

    /**
     * Success if term is grounded
     *
     * @param environment Execution environment
     * @param term        Term to test
     */
    @Predicate("ground")
    public static void ground(Environment environment, Term term) {
        // Wrapper already resolved term
        if (!term.isGrounded()) {
            environment.backtrack();
        }
    }

    /**
     * Collect all variables in specified term
     *
     * @param environment Execution environment
     * @param term        Term to analyze
     * @param varList     Collected variables
     */
    @Predicate("term_variables")
    public static void termVariables(Environment environment, Term term, Term varList) {
        // Extract all the variables out of term as a list
        List<? extends Variable> collected = collectVariables(environment, term);
        Unifier.unifyList(environment, varList, TermList.from(collected));
    }

    /**
     * Collect all variables in specified term, perform a diff
     *
     * @param environment Execution environment
     * @param term        Term to analyze
     * @param headList    Provided with a set of variables known/assumed
     * @param tailList    Additional variables not in headList
     */
    @Predicate("term_variables")
    public static void termVariables(Environment environment, Term term, Term headList, Term tailList) {
        Set<Long> vars = TermList.extractList(headList).stream().map(t -> t instanceof Variable ? ((Variable) t).id() : -1L)
                .collect(Collectors.toSet());
        List<? extends Variable> collected = collectVariables(environment, term);
        List<Variable> remaining = collected.stream().filter(t -> !vars.contains(t.id())).collect(Collectors.toList());
        Unifier.unifyList(environment, tailList, TermList.from(remaining));
    }

    /**
     * Replace variables with $VAR alternatives.
     *
     * @param environment Execution environment
     * @param term        Term to analyze
     * @param startTerm   First variable index
     * @param endTerm     Index of variable that would be next
     */
    @Predicate("numbervars")
    public static void numberVariables(Environment environment, Term term, Term startTerm, Term endTerm) {
        long index = PrologInteger.from(startTerm).toLong();
        List<? extends Variable> collected = collectVariables(environment, term);
        for (Variable t : collected) {
            Term vt = new CompoundTermImpl(Interned.DOLLAR_VAR, PrologInteger.from(index));
            if (t.instantiate(vt)) {
                index++;
            }
        }
        Unifier.unifyInteger(environment, endTerm, index);
    }

    /**
     * Success if var is a variable in term
     *
     * @param environment Execution environment
     * @param term        Term to test
     * @param var         Variable to test (may recurse)
     */
    @Predicate("nonground")
    public static void nonground(Environment environment, Term term, Term var) {
        // Wrapper already resolved term
        if (term.isGrounded()) {
            environment.backtrack();
        }
        Stream<? extends Variable> stream = collectVariables(environment, term).stream();
        if (stream.noneMatch(t -> t.compareTo(var) == 0)) {
            environment.backtrack();
        }
    }

    /**
     * Common utility to collect all variables out of source term
     *
     * @param environment Execution environment
     * @param sourceTerm  Term to extract variables out of
     * @return list of variable terms
     */
    private static List<? extends Variable> collectVariables(Environment environment, Term sourceTerm) {
        List<? extends Variable> collected;
        if (sourceTerm.isGrounded()) {
            collected = Collections.emptyList();
        } else {
            VariableCollector collector = new VariableCollector(environment, VariableCollector.Mode.COLLECT);
            sourceTerm.enumTerm(collector);
            collected = collector.getVariables();
        }
        return collected;
    }
}
