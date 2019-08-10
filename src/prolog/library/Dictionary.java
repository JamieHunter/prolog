// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Interned;
import prolog.bootstrap.Predicate;
import prolog.constants.Atomic;
import prolog.constants.PrologAtom;
import prolog.constants.PrologInteger;
import prolog.exceptions.PrologInstantiationError;
import prolog.exceptions.PrologPermissionError;
import prolog.exceptions.PrologTypeError;
import prolog.execution.CompileContext;
import prolog.execution.CopyTermContext;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.instructions.ExecExhaust;
import prolog.instructions.ExecFindClause;
import prolog.instructions.ExecRetractClause;
import prolog.predicates.ClauseEntry;
import prolog.predicates.ClauseSearchPredicate;
import prolog.predicates.MissingPredicate;
import prolog.predicates.PredicateDefinition;
import prolog.predicates.Predication;
import prolog.unification.Unifier;
import prolog.unification.UnifyBuilder;

import java.util.function.BiConsumer;

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
     * Abolish a predicate with given functor and arity specified as terms
     *
     * @param environment Execution environment
     * @param functor     Functor atom
     * @param arity       Lambda integer
     */
    @Predicate("abolish")
    public static void abolish(Environment environment, Term functor, Term arity) {
        PrologAtom functorAtom = PrologAtom.from(functor);
        PrologInteger arityInt = PrologInteger.from(arity);
        Predication predication = new Predication(functorAtom, arityInt.get().intValue());
        PredicateDefinition defn =
                environment.lookupPredicate(predication);
        if (defn instanceof MissingPredicate) {
            return; // already abolished
        }
        if (defn instanceof ClauseSearchPredicate) {
            environment.abolishPredicate(predication);
        } else {
            throw PrologPermissionError.error(environment, "modify", "static_procedure", predication.term(),
                    "Cannot abolish built-in procedure");
        }
    }

    /**
     * Recursively match clauses
     *
     * @param compiling Compilation context
     * @param term      The predicate template to retract
     */
    @Predicate(value = "retract", arity = 1)
    public static void retract(CompileContext compiling, CompoundTerm term) {
        Term clause = term.get(0);
        compiling.add(new ExecRetractClause(clause));
    }

    /**
     * Recursively match and retract clauses.
     *
     * @param compiling Compilation context
     * @param term      The predicate template to retract
     */
    @Predicate(value = "retractall", arity = 1)
    public static void retractAll(CompileContext compiling, CompoundTerm term) {
        compiling.add(
                new ExecExhaust(
                        compiling.environment(),
                        c2 -> retract(c2, term)));
    }

    /**
     * Recursively match clauses
     *
     * @param compiling Compilation context
     * @param term      The predicate to call
     */
    @Predicate(value = "clause", arity = 2)
    public static void clause(CompileContext compiling, CompoundTerm term) {
        Term head = term.get(0);
        Term body = term.get(1);
        compiling.add(new ExecFindClause(head, body));
    }

    /**
     * Mark a predication as dynamic
     * @param environment Execution environment
     * @param predicationTerm Specifier
     */
    @Predicate("dynamic")
    public static void dynamic(Environment environment, Term predicationTerm) {
        ClauseSearchPredicate predicate = lookupFromPredication(environment, predicationTerm);
        predicate.setDynamic(true);
    }

    /**
     * Mark a predication as multi-file (inhibit consult from deleting definitions)
     * @param environment Execution environment
     * @param predicationTerm Specifier
     */
    @Predicate("multifile")
    public static void multifile(Environment environment, Term predicationTerm) {
        ClauseSearchPredicate predicate = lookupFromPredication(environment, predicationTerm);
        predicate.setMultifile(true);
    }

    /**
     * Mark a predication that definitions might be discontiguous
     * @param environment Execution environment
     * @param predicationTerm Specifier
     */
    @Predicate("discontiguous")
    public static void discontiguous(Environment environment, Term predicationTerm) {
        ClauseSearchPredicate predicate = lookupFromPredication(environment, predicationTerm);
        // TODO: currently not used
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
        term = term.copyTerm(new CopyTermContext(environment));
        if (CompoundTerm.termIsA(term, Interned.CLAUSE_FUNCTOR, 2)) {
            // Rule
            CompoundTerm clause = (CompoundTerm) term;
            addClauseRule(environment, clause.get(0), clause.get(1), add, isDynamic);
        } else {
            // Fact
            addClauseRule(environment, term, Interned.TRUE_ATOM, add, isDynamic);
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
        head = head.value(environment);
        body = body.value(environment);
        if (!head.isInstantiated()) {
            throw PrologInstantiationError.error(environment, head);
        }
        if (head instanceof PrologAtom) {
            head = CompoundTerm.from((Atomic) head);
        }
        if (!(head instanceof CompoundTerm)) {
            throw PrologTypeError.callableExpected(environment, head);
        }
        CompoundTerm compoundHead = (CompoundTerm) head;
        Unifier unifier = UnifyBuilder.from(compoundHead);
        Predication predication = new Predication(compoundHead.functor(), compoundHead.arity());
        // create library entry prior to compiling
        ClauseSearchPredicate dictionaryEntry =
                environment.createDictionaryEntry(predication);
        // this causes the reconsult type semantics when consulting
        dictionaryEntry.changeLoadGroup(environment.getLoadGroup());

        // compile body
        CompileContext compiling = new CompileContext(environment);
        body.compile(compiling);
        Instruction compiled = compiling.toInstruction();
        // add clause to library
        ClauseEntry entry = new ClauseEntry((CompoundTerm)head, body, unifier, compiled);
        add.accept(dictionaryEntry, entry);
    }

    private static ClauseSearchPredicate lookupFromPredication(Environment environment, Term predicationTerm) {
        if (!CompoundTerm.termIsA(predicationTerm, Interned.SLASH_ATOM, 2)) {
            // TODO: better error?
            throw PrologTypeError.compoundExpected(environment, predicationTerm);
        }
        CompoundTerm predicationCompound = (CompoundTerm)predicationTerm;
        Term functor = predicationCompound.get(0);
        Term arity = predicationCompound.get(1);
        PrologAtom functorAtom = PrologAtom.from(functor);
        PrologInteger arityInt = PrologInteger.from(arity);
        Predication predication = new Predication(functorAtom, arityInt.get().intValue());
        // create library entry if needed
        PredicateDefinition entry = environment.autoCreateDictionaryEntry(predication);
        if (!(entry instanceof ClauseSearchPredicate)) {
            throw PrologPermissionError.error(environment, "modify", "static_procedure", predication.term(),
                    "Cannot make procedure dynamic");
        }
        return (ClauseSearchPredicate)entry;
    }
}
