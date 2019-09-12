// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Interned;
import prolog.bootstrap.Predicate;
import prolog.constants.PrologInteger;
import prolog.enumerators.VariableCollector;
import prolog.execution.Environment;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;
import prolog.expressions.TermList;
import prolog.generators.YieldSolutions;
import prolog.unification.Unifier;
import prolog.variables.Variable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
        List<Term> collected = collectVariables(environment, term);
        if (!Unifier.unify(environment.getLocalContext(), varList, TermList.from(collected))) {
            environment.backtrack();
        }
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
        HashSet<Long> vars = new HashSet<>();
        vars.addAll(TermList.extractList(headList).stream().map(t -> t instanceof Variable ? ((Variable)t).id() : -1L)
                .collect(Collectors.toList()));
        List<Term> collected = collectVariables(environment, term);
        List<Term> remaining = collected.stream().filter(t -> !vars.contains(((Variable)t).id())).collect(Collectors.toList());
        if (!Unifier.unify(environment.getLocalContext(), tailList, TermList.from(remaining))) {
            environment.backtrack();
        }
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
        List<Term> collected = collectVariables(environment, term);
        for (Term t : collected) {
            Term vt = new CompoundTermImpl(Interned.DOLLAR_VAR, PrologInteger.from(index));
            if (t.instantiate(vt)) {
                index++;
            }
        }
        if (!Unifier.unify(environment.getLocalContext(), endTerm, PrologInteger.from(index))) {
            environment.backtrack();
        }
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
        Stream<Term> stream = collectVariables(environment, term).stream();
        if(!stream.filter(t -> t.compareTo(var) == 0).findFirst().isPresent()) {
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
    private static List<Term> collectVariables(Environment environment, Term sourceTerm) {
        List<Term> collected;
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
