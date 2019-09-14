// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Builtins;
import org.jprolog.bootstrap.DemandLoad;
import org.jprolog.bootstrap.Predicate;
import org.jprolog.constants.Atomic;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.constants.PrologFloat;
import org.jprolog.constants.PrologNumber;
import org.jprolog.constants.PrologString;
import org.jprolog.exceptions.PrologTypeError;
import org.jprolog.execution.CompileContext;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.expressions.Term;
import org.jprolog.instructions.ExecBlock;
import org.jprolog.instructions.ExecCallLocal;
import org.jprolog.instructions.ExecFinally;
import org.jprolog.predicates.LoadGroup;
import org.jprolog.predicates.Predication;
import org.jprolog.unification.Unifier;
import org.jprolog.utility.LinkNode;
import org.jprolog.io.LogicalStream;
import org.jprolog.io.Prompt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;

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
     * Set load group. Restore it under any exist circumstance.
     *
     * @param environment Execution environment
     * @param idTerm      Id of load group
     * @param callable    Callable term
     */
    @Predicate("$load_group_scope")
    public static void setLoadGroup(Environment environment, Term idTerm, Term timeTerm, Term callable) {
        final String newId = convertId(environment, idTerm);
        final PrologFloat time = convertTime(environment, timeTerm);
        final LoadGroup priorGroup = environment.getLoadGroup();
        final LoadGroup newGroup = new LoadGroup(newId, time);

        new ExecFinally(ExecBlock.deferred(callable),
                e -> {
                    environment.changeLoadGroup(newGroup);
                },
                e -> {
                    environment.changeLoadGroup(priorGroup);
                }).invoke(environment);
    }

    /**
     * Add path to search path, remove it under any exit circumstance.
     *
     * @param environment Execution environment
     * @param pathTerm    Search path to add
     * @param callable    Callable term
     */
    @Predicate("$file_search_scope")
    public static void fileSearchScope(Environment environment, Term pathTerm, Term callable) {
        Path path = Io.parsePathWithCWD(environment, pathTerm);
        if (!Files.isDirectory(path)) {
            path = path.getParent();
        }
        final LinkNode node = new LinkNode(path);

        new ExecFinally(ExecBlock.deferred(callable),
                e -> {
                    environment.getSearchPath().addHead(node);
                },
                e -> {
                    node.remove();
                }).invoke(environment);
    }

    /**
     * Add goal to list of things to execute after text is loaded
     *
     * @param environment Execution environment
     * @param goal        Goal to execute
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
        if (group == null) {
            return;
        }
        List<Term> initialization = group.getInitialization();

        // Compile all the terms as if they were provided as a single ':-' at the end of the script
        CompileContext compiling = environment.newCompileContext();
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
            Builtins.predicate("load_files", 2),
            Builtins.predicate("load_files", 1),
            // equivalent of load_files(File, []) except for handling special file user
            Builtins.predicate("consult", 1),
            // consult list of files
            Builtins.predicate(".", 2),
            // equivalent to load_files(File, [if(not_loaded)])
            Builtins.predicate("ensure_loaded", 1),
            // inline insertion of file
            Builtins.predicate("include", 1)
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
