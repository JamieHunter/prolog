// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Predicate;
import prolog.constants.Atomic;
import prolog.exceptions.PrologInstantiationError;
import prolog.exceptions.PrologTypeError;
import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.flags.CreateFlagOptions;
import prolog.flags.PrologFlags;
import prolog.unification.Unifier;

/**
 * Library facility for Prolog flag manipulation/querying
 */
public class Flags {
    @Predicate("current_prolog_flag")
    public static void currentPrologFlag(Environment environment, Term key, Term value) {
        if (!key.isAtomic()) {
            throw PrologTypeError.atomExpected(environment, key);
        }
        PrologFlags flags = environment.getFlags();
        Term actual = flags.get((Atomic)key);
        if (!Unifier.unify(environment.getLocalContext(), value, actual)) {
            environment.backtrack();
        }
    }

    @Predicate("set_prolog_flag")
    public static void setPrologFlag(Environment environment, Term key, Term value) {
        if (!key.isAtomic()) {
            throw PrologTypeError.atomExpected(environment, key);
        }
        if (!value.isGrounded()) {
            throw PrologInstantiationError.error(environment, value);
        }
        PrologFlags flags = environment.getFlags();
        flags.set((Atomic)key, value);
    }

    @Predicate("create_prolog_flag")
    public static void createPrologFlag(Environment environment, Term key, Term value, Term optionsTerm) {
        if (!key.isAtomic()) {
            throw PrologTypeError.atomExpected(environment, key);
        }
        if (!value.isGrounded()) {
            throw PrologInstantiationError.error(environment, value);
        }
        PrologFlags flags = environment.getFlags();
        CreateFlagOptions options = new CreateFlagOptions(environment, optionsTerm);
        flags.create((Atomic)key, value, options);
    }
}
