// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Interned;
import org.jprolog.bootstrap.Predicate;
import org.jprolog.constants.PrologAtomInterned;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.constants.PrologEmptyList;
import org.jprolog.constants.PrologInteger;
import org.jprolog.debugging.SpySpec;
import org.jprolog.exceptions.PrologDomainError;
import org.jprolog.exceptions.PrologInstantiationError;
import org.jprolog.exceptions.PrologSyntaxError;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.Term;
import org.jprolog.expressions.TermList;
import org.jprolog.flags.WriteOptions;
import org.jprolog.io.LogicalStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

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
        if (!leashOptions.isInstantiated()) {
            // report leashing
            Term term = environment.spyPoints().getLeashFlags(environment);
            if (!leashOptions.instantiate(term)) {
                environment.backtrack();
            }
            return;
        }
        if (leashOptions == PrologEmptyList.EMPTY_LIST) {
            environment.spyPoints().setLeashFlags(0);
        }
        List<Term> ports = TermList.extractList(leashOptions);
        int includeMask = 0;
        int excludeMask = 0;
        int testMask = 0;
        for (Term t : ports) {
            int flagType = 0;
            if (t instanceof CompoundTerm) {
                CompoundTerm tt = (CompoundTerm) t;
                if (tt.arity() != 1) {
                    throw PrologSyntaxError.error(environment, "port", "Unable to interpret " + tt.toString());
                }
                String prefixString = PrologAtomLike.from(tt.functor()).name();
                if (prefixString.equals("-")) {
                    flagType = -1;
                } else if (prefixString.equals("+")) {
                    flagType = 1;
                } else {
                    throw PrologDomainError.error(environment, "lease_port", t);
                }
                t = tt.get(0);
            }
            PrologAtomLike atom = PrologAtomLike.from(t);
            String flag = atom.name();
            int mask = environment.spyPoints().parseLeashFlag(flag);
            if (flagType > 0) {
                includeMask |= mask;
            } else if (flagType < 0) {
                excludeMask |= mask;
            } else {
                includeMask |= mask;
                excludeMask = -1;
            }
        }
        int flags = environment.spyPoints().getLeashFlags();
        if (testMask != 0) {
            // TODO!
            if ((testMask & flags) != flags) {
                environment.backtrack();
                return;
            }
        }
        flags = flags & ~excludeMask;
        flags = flags | includeMask;
        environment.spyPoints().setLeashFlags(flags);
    }

    /**
     * Give a summary of debugging state
     *
     * @param environment Execution environment
     */
    @Predicate(value = "debugging", notrace = true)
    public static void debugging(Environment environment) {
        debugging(environment, environment.getOutputStream());
    }

    public static void debugging(Environment environment, LogicalStream logicalStream) {
        if (environment.isDebuggerEnabled()) {
            logicalStream.write(environment, null, "Debugging is enabled.\n");
        } else {
            logicalStream.write(environment, null, "Debugging is disabled.\n");
        }
        Term leash = environment.spyPoints().getLeashFlags(environment);
        if (leash == PrologEmptyList.EMPTY_LIST) {
            logicalStream.write(environment, null, "Leash ports: [] (tracing is disabled).\n");
        } else {
            logicalStream.write(environment, null, "Leash ports: ");
            WriteOptions op = new WriteOptions(environment, null);
            op.nl = true;
            logicalStream.write(environment, null, leash, op);
        }
        TreeSet<String> spies = new TreeSet<>();
        for (SpySpec spec : environment.spyPoints().enumerate()) {
            spies.add(spec.toString());
        }
        if (spies.size() == 0) {
            logicalStream.write(environment, null, "No spy points are set.\n");
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("Spy points:");
            for (String ref : spies) {
                builder.append(" ");
                builder.append(ref);
            }
            builder.append('\n');
            logicalStream.write(environment, null, builder.toString());
        }
    }

}
