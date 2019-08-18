// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.DemandLoad;
import prolog.bootstrap.Predicate;
import prolog.constants.Atomic;
import prolog.constants.PrologAtom;
import prolog.constants.PrologAtomLike;
import prolog.constants.PrologFloat;
import prolog.constants.PrologNumber;
import prolog.constants.PrologString;
import prolog.exceptions.PrologTypeError;
import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.expressions.Term;
import prolog.instructions.ExecCallLocal;
import prolog.io.LogicalStream;
import prolog.io.Prompt;
import prolog.predicates.LoadGroup;
import prolog.predicates.Predication;
import prolog.unification.Unifier;

import java.util.List;
import java.util.ListIterator;

import static prolog.bootstrap.Builtins.predicate;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Bootstraps consult. Consult is predominantly implemented as a Prolog script that needs to be bootstrapped.
 */
public final class Consult {
    private Consult() {
        // Static methods/fields only
    }

    /**
     * Primes interactive prompt to indicate interactive consult.
     *
     * @param environment Execution environment
     * @param streamIdent Single term specifying stream.
     */
    @Predicate("$set_prompt_consult")
    public static void setPromptConsult(Environment environment, Term streamIdent) {
        LogicalStream logicalStream = Io.lookupStream(environment, streamIdent);
        logicalStream.setPrompt(environment, (Atomic) streamIdent, Prompt.CONSULT);
    }

    /**
     * Ends interactive prompt.
     *
     * @param environment Execution environment
     * @param streamIdent Single term specifying stream.
     */
    @Predicate("$set_prompt_none")
    public static void setPromptNone(Environment environment, Term streamIdent) {
        LogicalStream logicalStream = Io.lookupStream(environment, streamIdent);
        logicalStream.setPrompt(environment, (Atomic) streamIdent, Prompt.NONE);
    }

    /**
     * Given a load group identifier, determines if a load group by that identifier exists. If so,
     * unify the date/time. If not, fail.
     *
     * @param environment Execution environment
     * @param idTerm      Id of group
     * @param timeTerm    Target of Date/Time (unified)
     */
    @Predicate("$get_load_group_time")
    public static void getLoadGroupExists(Environment environment, Term idTerm, Term timeTerm) {
        String id = convertId(environment, idTerm);
        LoadGroup group = environment.getLoadGroup(id);
        if (group == null) {
            environment.backtrack();
            return;
        }
        PrologFloat time = group.getTime();
        if (!Unifier.unify(environment.getLocalContext(), timeTerm, time)) {
            environment.backtrack();
        }
    }

    /**
     * Create a new load group and establish as current
     *
     * @param environment    Execution environment
     * @param idTerm         Id of new group
     * @param timeTerm       Time of new group if instantiated
     * @param priorGroupTerm Unified with prior group for later restore
     */
    @Predicate("$begin_load_group")
    public static void beginLoadGroup(Environment environment, Term idTerm, Term timeTerm, Term priorGroupTerm) {
        String id = convertId(environment, idTerm);
        PrologFloat time = convertTime(environment, timeTerm);
        LoadGroup priorGroup = environment.getLoadGroup();
        PrologAtom priorIdAtom = new PrologAtom(priorGroup.getId());
        if (!Unifier.unify(environment.getLocalContext(), priorGroupTerm, priorIdAtom)) {
            environment.backtrack();
            return;
        }
        environment.changeLoadGroup(new LoadGroup(id, time));
    }

    /**
     * Select and restore a previously known load group
     *
     * @param environment Execution environment
     * @param idTerm      Id of prior group
     */
    @Predicate("$restore_load_group")
    public static void setLoadGroup(Environment environment, Term idTerm) {
        String id = convertId(environment, idTerm);
        LoadGroup group = environment.getLoadGroup(id);
        if (group == null) {
            environment.backtrack();
            return;
        }
        environment.changeLoadGroup(group);
    }

    /**
     * Add goal to list of things to execute after text is loaded
     *
     * @param environment
     * @param goal
     */
    @Predicate("initialization")
    public static void initialization(Environment environment, Term goal) {
        LoadGroup group = environment.getLoadGroup();
        if (group.getId().length() > 0) {
            group.addInitialization(goal);
        }
    }

    /**
     * Execute all the initialization goals
     *
     * @param environment Execution environment
     * @param idTerm      Id of load group
     */
    @Predicate("$do_initialization")
    public static void doInitialization(Environment environment, Term idTerm) {
        String id = convertId(environment, idTerm);
        if (id.length() == 0) {
            return;
        }
        LoadGroup group = environment.getLoadGroup(id);
        List<Term> initialization = group.getInitialization();

        // Compile all the terms as if they were provided as a single ':-' at the end of the script
        CompileContext compiling = new CompileContext(environment);
        ListIterator<Term> iter = initialization.listIterator();
        while (iter.hasNext()) {
            Term term = iter.next();
            iter.remove();
            term.compile(compiling);
        }
        Instruction sequence = compiling.toInstruction();
        Instruction nested = new ExecCallLocal(environment, sequence);
        nested.invoke(environment);
    }

    /**
     * List of predicates defined by the resource "consult.pl".
     */
    @DemandLoad("consult.pl")
    public static Predication consult[] = {
            // parent for all loading predicates except include
            // note there are a lot of options to this predicate
            predicate("load_files", 2),
            predicate("load_files", 1),
            // equivalent of load_files(File, []) except for handling special file user
            predicate("consult", 1),
            // consult list of files
            predicate(".", 2),
            // equivalent to load_files(File, [if(not_loaded)])
            predicate("ensure_loaded", 1),
            // inline insertion of file
            predicate("include", 1)
    };

    // ====================================================================
    // Helper methods
    // ====================================================================

    /**
     * Verify and retrieve identifier
     *
     * @param id Id parameter
     * @return validated id
     */
    private static String convertId(Environment environment, Term id) {
        String pathName;
        if (id.isAtom()) {
            return ((PrologAtomLike) (id.value(environment))).name();
        } else if (id.isString()) {
            return ((PrologString) (id.value(environment))).get();
        } else {
            throw PrologTypeError.atomExpected(environment, id);
        }
    }

    /**
     * Verify and retrieve time. If time is unbound, current time is used
     *
     * @param time Time parameter
     * @return validated time
     */
    private static PrologFloat convertTime(Environment environment, Term time) {
        if (!time.isInstantiated()) {
            return Time.now();
        }
        if (!(time instanceof PrologNumber)) {
            throw PrologTypeError.numberExpected(environment, time);
        }
        return ((PrologNumber) time).toPrologFloat();
    }

}
