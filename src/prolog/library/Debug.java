// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Interned;
import prolog.bootstrap.Predicate;
import prolog.constants.PrologAtomInterned;
import prolog.constants.PrologInteger;
import prolog.debugging.DebugStateChange;
import prolog.debugging.SpySpec;
import prolog.exceptions.PrologDomainError;
import prolog.exceptions.PrologInstantiationError;
import prolog.execution.Environment;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.expressions.TermList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Debugging utilities
 */
public final class Debug {
    private Debug() {
        // Static methods/fields only
    }

    /**
     * Enable debugger
     *
     * @param environment Execution environment
     */
    @Predicate(value = "debug", notrace = true)
    public static void debug(Environment environment) {
        if (!environment.isDebuggerEnabled()) {
            environment.enableDebugger(true);
            throw new DebugStateChange(); // kick execution loop out of optimal loop
        }
    }

    /**
     * Enable debugger, begin tracing
     *
     * @param environment Execution environment
     */
    @Predicate(value = "trace", notrace = true)
    public static void trace(Environment environment) {
        if (!environment.isDebuggerEnabled()) {
            environment.enableDebugger(true);
            environment.debugger().trace();
            throw new DebugStateChange(); // kick execution loop out of optimal loop
        } else {
            environment.debugger().trace();
        }
    }

    /**
     * Disable debugger
     *
     * @param environment Execution environment
     */
    @Predicate(value = {"nodebug", "notrace"}, notrace = true)
    public static void nodebug(Environment environment) {
        if (!environment.isDebuggerEnabled()) {
            environment.enableDebugger(false);
        }
    }

    /**
     * Spy
     *
     * @param environment Execution environment
     * @param specTerm    Specification
     */
    @Predicate(value = "spy", notrace = true)
    public static void spy(Environment environment, Term specTerm) {
        List<SpySpec> list = expandSpySpec(environment, specTerm);
        list.forEach(environment.spyPoints()::addSpy);
    }

    /**
     * Disable Spy
     *
     * @param environment Execution environment
     * @param specTerm    Specification
     */
    @Predicate(value = "nospy", notrace = true)
    public static void nospy(Environment environment, Term specTerm) {
        List<SpySpec> list = expandSpySpec(environment, specTerm);
        list.forEach(environment.spyPoints()::removeSpy);
    }

    private static List<SpySpec> expandSpySpec(Environment environment, Term specTerm) {
        ArrayList<SpySpec> list = new ArrayList<>();
        if (!specTerm.isInstantiated()) {
            throw PrologInstantiationError.error(environment, specTerm);
        }
        List<Term> terms;
        if (TermList.isList(specTerm)) {
            terms = TermList.extractList(specTerm);
        } else {
            terms = Arrays.asList(specTerm);
        }
        for (Term term : terms) {
            list.add(expandSingleSpySpec(environment, term));
        }
        return list;
    }

    private static SpySpec expandSingleSpySpec(Environment environment, Term specTerm) {
        if (specTerm.isAtom()) {
            PrologAtomInterned atom = PrologAtomInterned.from(environment, specTerm);
            return SpySpec.from(atom);
        } else if (CompoundTerm.termIsA(specTerm, Interned.SLASH_ATOM, 2)) {
            CompoundTerm slashTerm = (CompoundTerm) specTerm;
            PrologAtomInterned atom = PrologAtomInterned.from(environment, slashTerm.get(0));
            PrologInteger arity = PrologInteger.from(slashTerm.get(1));
            return SpySpec.from(atom, arity.toInteger());
        } else {
            throw PrologDomainError.error(environment, "spy_spec", specTerm);
        }
    }

    /**
     * Clear Spy
     *
     * @param environment Execution environment
     */
    @Predicate(value = "nospyall", notrace = true)
    public static void nospyall(Environment environment) {
        environment.spyPoints().removeAll();
    }

    /**
     * Leash
     *
     * @param environment  Execution environment
     * @param leashOptions Specification
     */
    @Predicate(value = "leash", notrace = true)
    public static void leash(Environment environment, Term leashOptions) {
        throw new UnsupportedOperationException("NYI-leash");
    }

    /**
     * Give a summary of debugging state
     *
     * @param environment Execution environment
     */
    @Predicate(value = "debugging", notrace = true)
    public static void debugging(Environment environment) {
        throw new UnsupportedOperationException("NYI-debugging");
    }

}
