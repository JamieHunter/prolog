// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Interned;
import org.jprolog.bootstrap.Predicate;
import org.jprolog.constants.PrologAtomInterned;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.constants.PrologInteger;
import org.jprolog.enumerators.CallifyTerm;
import org.jprolog.enumerators.CopySimpleTerm;
import org.jprolog.exceptions.PrologInstantiationError;
import org.jprolog.exceptions.PrologPermissionError;
import org.jprolog.exceptions.PrologTypeError;
import org.jprolog.execution.Environment;
import org.jprolog.execution.LocalContext;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.Term;
import org.jprolog.generators.DoRedo;
import org.jprolog.generators.YieldSolutions;
import org.jprolog.predicates.BuiltInPredicate;
import org.jprolog.predicates.ClauseEntry;
import org.jprolog.predicates.ClauseSearchPredicate;
import org.jprolog.predicates.PredicateDefinition;
import org.jprolog.predicates.Predication;
import org.jprolog.unification.Unifier;
import org.jprolog.unification.UnifyBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Bootstraps dictionary related predicates.
 */
public final class Dictionary {
    private Dictionary() {
        // Static methods/fields only
    }

    /**
     * Add clause to start of dictionary.
     *
     * @param environment Execution environment
     * @param clause      Clause to add
     */
    @Predicate("asserta")
    public static void asserta(Environment environment, Term clause) {
        addClause(environment, clause, ClauseSearchPredicate::addStart, true);
    }

    /**
     * Add clause to end of dictionary.
     *
     * @param environment Execution environment
     * @param clause      Clause to add
     */
    @Predicate({"assert", "assertz"})
    public static void assertz(Environment environment, Term clause) {
        addClause(environment, clause, ClauseSearchPredicate::addEnd, true);
    }

    /**
     * Add clause to end of dictionary for consult
     * TODO: temporary, requires more work in consult.pl for optimization.
     *
     * @param environment Execution environment
     * @param clause      Clause to add
     */
    @Predicate("$consult_assertz")
    public static void consultAssertz(Environment environment, Term clause) {
        addClause(environment, clause, ClauseSearchPredicate::addEnd, false);
    }


    /**
     * Abolish a predicate with given functor and arity specified as terms
     *
     * @param environment Execution environment
     * @param predicator  functor/arity
     */
    @Predicate("abolish")
    public static void abolish(Environment environment, Term predicator) {
        if (!predicator.isInstantiated()) {
            throw PrologInstantiationError.error(environment, predicator);
        }
        if (!CompoundTerm.termIsA(predicator, Interned.SLASH_ATOM, 2)) {
            throw PrologTypeError.predicateIndicatorExpected(environment, predicator);
        }
        CompoundTerm comp = (CompoundTerm) predicator;
        abolish(environment, comp.get(0), comp.get(1));
    }

    /**
     * Abolish a predicate with given functor and arity specified as terms
     *
     * @param environment Execution environment
     * @param functor     Functor atom
     * @param arity       Lambda integer
     */
    @Predicate("abolish")
    public static void abolish(Environment environment, Term functor, Term arity) {
        if (!functor.isInstantiated()) {
            throw PrologInstantiationError.error(environment, functor);
        }
        if (!arity.isInstantiated()) {
            throw PrologInstantiationError.error(environment, functor);
        }
        PrologAtomInterned functorAtom = PrologAtomInterned.from(environment, functor);
        int arityInt = PrologInteger.from(arity).notLessThanZero().toArity(environment);
        Predication.Interned predication = new Predication.Interned(functorAtom, arityInt);
        PredicateDefinition defn =
                environment.lookupPredicate(predication);
        if (defn instanceof BuiltInPredicate) {
            // TODO: There can be some ClauseSearchPredicate procedures that are also considered static
            throw PrologPermissionError.error(environment,
                    Interned.MODIFY_ACTION, Interned.STATIC_PROCEDURE_TYPE, predication.term(),
                    String.format("Cannot retrieve clause for static procedure: %s", predication.toString()));
        }
        if (!(defn instanceof ClauseSearchPredicate)) {
            return; // silent
        }
        environment.abolishPredicate(predication);
    }

    /**
     * Match clauses, with backtracking
     *
     * @param environment Execution environment
     * @param clause      The clause to retract
     */
    @Predicate("retract")
    public static void retract(Environment environment, Term clause) {
        retractCommon(environment, clause, false);
    }

    /**
     * Recursively match and retract clauses.
     *
     * @param environment Execution environment
     * @param clause      The clause to retract
     */
    @Predicate("retractall")
    public static void retractAll(Environment environment, Term clause) {
        retractCommon(environment, clause, true);
    }

    /**
     * Retract and retract-all common behavior
     *
     * @param environment Execution environment
     * @param clause      The clause to retract
     */
    private static void retractCommon(Environment environment, Term clause, boolean all) {
        Term head;
        Term body;
        if (CompoundTerm.termIsA(clause, Interned.CLAUSE_FUNCTOR, 2)) {
            CompoundTerm compound = (CompoundTerm) clause;
            head = compound.get(0);
            body = compound.get(1);
        } else {
            head = clause;
            body = Interned.TRUE_ATOM; // fact
        }
        CompoundTerm headMatcher;

        //
        // Head must be sufficiently instantiated to build a matcher
        //
        if (!head.isInstantiated()) {
            throw PrologInstantiationError.error(environment, head);
        }
        if (head.isAtom()) {
            // Normalize matcher to a compound of arity 0
            headMatcher = CompoundTerm.from((PrologAtomLike) head);
        } else if (head instanceof CompoundTerm) {
            headMatcher = (CompoundTerm) head;
        } else {
            throw PrologTypeError.callableExpected(environment, head);
        }
        Predication predication = headMatcher.toPredication();
        // This may create a dynamic predicate in the process
        PredicateDefinition defn = environment.autoCreateDictionaryEntry(predication);
        if (!(defn instanceof ClauseSearchPredicate)) {
            // TODO: There can be some ClauseSearchPredicate procedures that are also considered static
            throw PrologPermissionError.error(environment,
                    Interned.MODIFY_ACTION, Interned.STATIC_PROCEDURE_TYPE, predication.term(),
                    String.format("Cannot retrieve clause for static procedure: %s", predication.toString()));
        }
        ClauseSearchPredicate clausePredicate = (ClauseSearchPredicate) defn;
        List<ClauseEntry> clauses = Arrays.asList(clausePredicate.getClauses());
        if (clauses.isEmpty() && all) {
            // is this by spec?
            clausePredicate.setDynamic(true);
            return;
        }
        if (clauses.isEmpty()) {
            environment.backtrack();
            return;
        }

        Unifier bodyUnifier = UnifyBuilder.from(body); // already resolved to context

        java.util.function.Predicate<ClauseEntry> clauseAction = entry -> {
            // use an alternative context in case the variable id's overlap
            LocalContext newContext = environment.newLocalContext(predication);

            Term boundBody = entry.getBody().resolve(newContext);

            // Attempt to unify
            Unifier headUnifier = entry.getUnifier();
            if (headUnifier.unify(newContext, headMatcher) &&
                    bodyUnifier.unify(newContext, boundBody)) {
                // Once unified, remove!
                entry.getNode().remove();
                return !all;
            } else {
                return false;
            }
        };
        if (all) {
            DoRedo.invoke(environment,
                    () -> YieldSolutions.forAll(environment, clauses.stream(), clauseAction),
                    () -> {
                        // success
                    }
            );
        } else {
            // retract a single clause
            YieldSolutions.forAll(environment, clauses.stream(), clauseAction);
        }
    }

    /**
     * Recursively match clauses
     *
     * @param environment Execution Environment
     * @param head        Head to match
     * @param body        Body to match
     */
    @Predicate("clause")
    public static void clause(Environment environment, Term head, Term body) {
        CompoundTerm headMatcher;

        //
        // Head must be sufficiently instantiated to build a matcher
        //
        if (!head.isInstantiated()) {
            throw PrologInstantiationError.error(environment, head);
        }
        if (head.isAtom()) {
            // Normalize matcher to a compound of arity 0
            headMatcher = CompoundTerm.from((PrologAtomLike) head);
        } else if (head instanceof CompoundTerm) {
            headMatcher = (CompoundTerm) head;
        } else {
            throw PrologTypeError.callableExpected(environment, head);
        }

        //
        // Body, if instantiated, must be something that can be called
        //
        if (body.isInstantiated() && !(body.isAtom() || body instanceof CompoundTerm)) {
            throw PrologTypeError.callableExpected(environment, body);
        }
        Predication predication = headMatcher.toPredication();
        PredicateDefinition defn = environment.lookupPredicate(predication);
        if (defn instanceof BuiltInPredicate) {
            throw PrologPermissionError.error(environment,
                    Interned.ACCESS_ACTION, Interned.PRIVATE_PROCEDURE_TYPE, predication.term(),
                    String.format("Cannot retrieve clause for internal procedure: %s", predication.toString()));
        }
        if (!(defn instanceof ClauseSearchPredicate)) {
            environment.backtrack();
            return;
        }
        ClauseSearchPredicate clausePredicate = (ClauseSearchPredicate) defn;
        List<ClauseEntry> clauses = Arrays.asList(clausePredicate.getClauses());
        Unifier bodyUnifier = UnifyBuilder.from(body); // already resolved to context

        java.util.function.Predicate<ClauseEntry> clauseAction = entry -> {
            // use an alternative context in case the variable id's overlap
            LocalContext newContext = environment.newLocalContext(predication);

            Term boundBody = entry.getBody().resolve(newContext);

            // Attempt to unify
            Unifier headUnifier = entry.getUnifier();
            if (headUnifier.unify(newContext, headMatcher) &&
                    bodyUnifier.unify(newContext, boundBody)) {
                return true;
            } else {
                return false;
            }
        };
        YieldSolutions.forAll(environment, clauses.stream(), clauseAction);
    }

    /**
     * Recursively determine user-defined procedures.
     *
     * @param environment Execution environment
     * @param indicator   Predicate indicator
     */
    @Predicate("current_predicate")
    public static void currentPredicate(Environment environment, Term indicator) {
        LocalContext context = environment.getLocalContext();
        Term bound = indicator.resolve(context);
        PrologAtomLike functorConstraint = null;
        Integer arityConstraint = null;
        if (bound.isInstantiated()) {
            if (!CompoundTerm.termIsA(bound, Interned.SLASH_ATOM, 2)) {
                throw PrologTypeError.predicateIndicatorExpected(environment, bound);
            }
            CompoundTerm compoundTerm = (CompoundTerm) bound;
            Term functor = compoundTerm.get(0);
            Term arity = compoundTerm.get(1);
            if (functor.isInstantiated()) {
                if (functor.isAtom()) {
                    functorConstraint = PrologAtomInterned.from(environment, functor);
                } else {
                    throw PrologTypeError.predicateIndicatorExpected(environment, bound);
                }
            }
            if (arity.isInstantiated()) {
                if (arity.isInteger()) {
                    arityConstraint = PrologInteger.from(arity).notLessThanZero().toInteger();
                } else {
                    throw PrologTypeError.predicateIndicatorExpected(environment, bound);
                }
            }
        }

        Set<Predication.Interned> predications = filterUserDefinedPredicates(environment, functorConstraint, arityConstraint)
                .collect(Collectors.toCollection(TreeSet::new));
        if (predications.isEmpty()) {
            environment.backtrack();
            return;
        }
        Unifier indicatorUnifier = UnifyBuilder.from(indicator);
        YieldSolutions.forAll(environment, predications.stream(),
                p -> indicatorUnifier.unify(environment.getLocalContext(), p.term()));
    }

    /**
     * Return a filtered stream of user defined predicates
     *
     * @param environment Execution environment
     * @param functor     Functor to filter, else null.
     * @param arity       Arity to filter, else null
     * @return stream of matching predications
     */
    private static Stream<Predication.Interned> filterUserDefinedPredicates(Environment environment, PrologAtomLike functor, Integer arity) {
        if (functor != null && arity != null) {
            Predication.Interned key = new Predication(functor, arity).intern(environment);
            PredicateDefinition singleDefinition = environment.lookupPredicate(key);
            if (singleDefinition == null || !singleDefinition.isCurrentPredicate()) {
                return Stream.empty();
            }
            return Stream.of(key);
        } else {
            return environment.getShared().allPredicates().filter(e ->
                    (functor == null || functor == e.getKey().functor()) &&
                            (arity == null || arity == e.getKey().arity()) &&
                            e.getValue().isCurrentPredicate())
                    .map(Map.Entry::getKey);
        }
    }

    /**
     * Mark a predication as dynamic
     *
     * @param environment     Execution environment
     * @param predicationTerm Specifier
     */
    @Predicate("dynamic")
    public static void dynamic(Environment environment, Term predicationTerm) {
        ClauseSearchPredicate predicate = lookupFromPredication(environment, predicationTerm);
        predicate.setDynamic(true);
    }

    /**
     * Mark a predication as multi-file (inhibit consult from deleting definitions)
     *
     * @param environment     Execution environment
     * @param predicationTerm Specifier
     */
    @Predicate("multifile")
    public static void multifile(Environment environment, Term predicationTerm) {
        ClauseSearchPredicate predicate = lookupFromPredication(environment, predicationTerm);
        predicate.setMultifile(true);
    }

    /**
     * Mark a predication that definitions might be discontiguous
     *
     * @param environment     Execution environment
     * @param predicationTerm Specifier
     */
    @Predicate("discontiguous")
    public static void discontiguous(Environment environment, Term predicationTerm) {
        ClauseSearchPredicate predicate = lookupFromPredication(environment, predicationTerm);
        predicate.setDiscontiguous(true);
    }

    // ==============================================
    // Helpers
    // ==============================================

    /**
     * Add clause from consultation.
     * TODO: Need file marker
     *
     * @param environment Execution environment
     * @param clause      Clause to add
     */
    public static void addClauseZ(Environment environment, Term clause) {
        addClause(environment, clause, ClauseSearchPredicate::addEnd, false);
    }

    /**
     * assert helper
     *
     * @param environment Environment
     * @param term        Term which is a clause or a fact
     * @param add         StackFunction to use to add (head/tail)
     * @param isDynamic   true if add is considered dynamic
     */
    private static void addClause(Environment environment, Term term,
                                  BiConsumer<ClauseSearchPredicate, ClauseEntry> add,
                                  boolean isDynamic) {
        // Copy done here, as the variables left and right of :- must relate to each other
        Term copy = term.enumTerm(new CopySimpleTerm(environment)); // removes ActiveVariable's
        if (CompoundTerm.termIsA(copy, Interned.CLAUSE_FUNCTOR, 2)) {
            // Rule
            CompoundTerm clause = (CompoundTerm) copy;
            Term head = clause.get(0);
            Term tail = clause.get(1).enumTerm(new CallifyTerm(environment, Optional.empty()));
            addClauseRule(environment, head, tail, add, isDynamic);
        } else {
            // Fact
            addClauseRule(environment, copy, Interned.TRUE_ATOM, add, isDynamic);
        }
    }

    /**
     * assert helper, add a clause, broken into head and body
     *
     * @param environment Environment
     * @param head        Head of clause
     * @param body        Body of clause
     * @param add         Add method
     * @param isDynamic   true if considered dynamic
     */
    private static void addClauseRule(Environment environment, Term head, Term body,
                                      BiConsumer<ClauseSearchPredicate, ClauseEntry> add,
                                      boolean isDynamic) {
        head = head.value();
        body = body.value();
        if (!head.isInstantiated()) {
            throw PrologInstantiationError.error(environment, head);
        }
        if (head.isAtom()) {
            head = CompoundTerm.from((PrologAtomLike) head);
        }
        if (!(head instanceof CompoundTerm)) {
            throw PrologTypeError.callableExpected(environment, head);
        }
        CompoundTerm compoundHead = (CompoundTerm) head;
        Unifier unifier = UnifyBuilder.from(compoundHead);
        Predication.Interned predication = new Predication.Interned(
                PrologAtomInterned.from(environment,
                        compoundHead.functor()), compoundHead.arity());
        // create library entry prior to compiling
        ClauseSearchPredicate dictionaryEntry =
                environment.createDictionaryEntry(predication);

        if (isDynamic) {
            if (!dictionaryEntry.isDynamic()) {
                if (dictionaryEntry.getClauses().length > 0) {
                    throw PrologPermissionError.error(environment, Interned.MODIFY_ACTION, Interned.STATIC_PROCEDURE_TYPE,
                            predication.term(),
                            "The predicate " + predication.toString() + " is a static procedure");
                }
                dictionaryEntry.setDynamic(true); // implied
            }
        } else {
            // this causes the reconsult type semantics when consulting
            dictionaryEntry.changeLoadGroup(environment.getLoadGroup());
        }

        // add clause to library (don't compile until execution)
        ClauseEntry entry = new ClauseEntry(environment.getShared(), (CompoundTerm) head, body, unifier);
        add.accept(dictionaryEntry, entry);
    }

    private static ClauseSearchPredicate lookupFromPredication(Environment environment, Term predicationTerm) {
        if (!CompoundTerm.termIsA(predicationTerm, Interned.SLASH_ATOM, 2)) {
            // TODO: better error?
            throw PrologTypeError.compoundExpected(environment, predicationTerm);
        }
        CompoundTerm predicationCompound = (CompoundTerm) predicationTerm;
        Term functor = predicationCompound.get(0);
        Term arity = predicationCompound.get(1);
        PrologAtomInterned functorAtom = PrologAtomInterned.from(environment, functor);
        PrologInteger arityInt = PrologInteger.from(arity);
        Predication.Interned predication = new Predication.Interned(functorAtom, arityInt.notLessThanZero().toInteger());
        // create library entry if needed
        PredicateDefinition entry = environment.autoCreateDictionaryEntry(predication);
        if (!(entry instanceof ClauseSearchPredicate)) {
            throw PrologPermissionError.error(environment, Interned.MODIFY_ACTION, Interned.STATIC_PROCEDURE_TYPE, predication.term(),
                    "Cannot make procedure dynamic");
        }
        return (ClauseSearchPredicate) entry;
    }
}
