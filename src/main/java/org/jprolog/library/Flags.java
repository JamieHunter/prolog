// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Predicate;
import org.jprolog.constants.Atomic;
import org.jprolog.exceptions.PrologInstantiationError;
import org.jprolog.exceptions.PrologTypeError;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.Term;
import org.jprolog.flags.CreateFlagOptions;
import org.jprolog.flags.PrologFlags;
import org.jprolog.generators.YieldSolutions;
import org.jprolog.unification.Unifier;

import java.util.Map;

/**
 * Library facility for Prolog flag manipulation/querying
 */
public class Flags {
    @Predicate("current_prolog_flag")
    public static void currentPrologFlag(Environment environment, Term key, Term value) {
        if (key.isInstantiated()) {
            if (!key.isAtom()) {
                throw PrologTypeError.atomExpected(environment, key);
            }
            PrologFlags flags = environment.getFlags();
            Term actual = flags.get(environment, (Atomic) key);
            Unifier.unifyTerm(environment, value, actual);
        } else {
            Map<Atomic, Term> allFlags = environment.getFlags().getAll(environment);
            YieldSolutions.forAll(environment, allFlags.entrySet().stream(), entry ->
                    Unifier.unifyTerm(environment, key, entry.getKey()) &&
                            Unifier.unifyTerm(environment, value, entry.getValue()));
        }
    }

    @Predicate("set_prolog_flag")
    public static void setPrologFlag(Environment environment, Term key, Term value) {
        if (!key.isInstantiated()) {
            throw PrologInstantiationError.error(environment, key);
        }
        if (!key.isAtom()) {
            throw PrologTypeError.atomExpected(environment, key);
        }
        if (!value.isGrounded()) {
            throw PrologInstantiationError.error(environment, value);
        }
        PrologFlags flags = environment.getFlags();
        flags.set(environment, (Atomic) key, value);
    }

    @Predicate("create_prolog_flag")
    public static void createPrologFlag(Environment environment, Term key, Term value, Term optionsTerm) {
        if (!key.isInstantiated()) {
            throw PrologInstantiationError.error(environment, key);
        }
        if (!key.isAtom()) {
            throw PrologTypeError.atomExpected(environment, key);
        }
        if (!value.isGrounded()) {
            throw PrologInstantiationError.error(environment, value);
        }
        PrologFlags flags = environment.getFlags();
        CreateFlagOptions options = new CreateFlagOptions(environment, optionsTerm);
        flags.create((Atomic) key, value, options);
    }
}
