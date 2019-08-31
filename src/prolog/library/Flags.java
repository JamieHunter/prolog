// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Predicate;
import prolog.constants.Atomic;
import prolog.debugging.InstructionReflection;
import prolog.exceptions.PrologInstantiationError;
import prolog.exceptions.PrologTypeError;
import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.expressions.CompoundTerm;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;
import prolog.flags.CreateFlagOptions;
import prolog.flags.PrologFlags;
import prolog.flags.StreamProperties;
import prolog.unification.Unifier;

import java.util.Iterator;
import java.util.Map;

/**
 * Library facility for Prolog flag manipulation/querying
 */
public class Flags {
    @Predicate("current_prolog_flag")
    public static void currentPrologFlag(Environment environment, Term key, Term value) {
        if (key.isInstantiated()) {
            if (!key.isAtomic()) {
                throw PrologTypeError.atomExpected(environment, key);
            }
            PrologFlags flags = environment.getFlags();
            Term actual = flags.get(environment, (Atomic) key);
            if (!Unifier.unify(environment.getLocalContext(), value, actual)) {
                environment.backtrack();
            }
        } else {
            Map<Atomic, Term> allFlags = environment.getFlags().getAll(environment);
            new ForEachFlag(environment, allFlags.entrySet().iterator(), key, value).next();
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
        flags.set(environment, (Atomic)key, value);
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

    private static class ForEachFlag extends DecisionPoint {
        private final Iterator<Map.Entry<Atomic, Term>> iter;
        private final Term key;
        private final Term value;

        private ForEachFlag(Environment environment, Iterator<Map.Entry<Atomic, Term>> iter, Term key, Term value) {
            super(environment);
            this.iter = iter;
            this.key = key;
            this.value = value;
        }

        @Override
        protected void next() {
            if (!iter.hasNext()) {
                environment.backtrack();
                return;
            }
            Map.Entry<Atomic, Term> entry = iter.next();
            environment.forward();
            if (iter.hasNext()) {
                environment.pushDecisionPoint(this);
            }
            LocalContext context = environment.getLocalContext();
            if (! (Unifier.unify(context, key, entry.getKey()) &&
                    Unifier.unify(context, value, entry.getValue()))) {
                environment.backtrack();
            }
        }
    }
}
