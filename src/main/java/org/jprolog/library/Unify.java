// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Predicate;
import org.jprolog.execution.CompileContext;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.Term;
import org.jprolog.functions.CompileMathExpression;
import org.jprolog.instructions.ExecIfThenElse;
import org.jprolog.instructions.ExecIs;
import org.jprolog.instructions.ExecUnifyCompounds;
import org.jprolog.instructions.ExecUnifyInstantiate;
import org.jprolog.variables.Variable;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Bootstraps unification predicates.
 */
public final class Unify {
    private Unify() {
        // Static methods/fields only
    }

    /**
     * Compiles unification to an applicable unify instruction
     *
     * @param compiling Compilation context
     * @param source      The '=' term
     */
    @Predicate(value = "=", arity = 2)
    public static void unify(CompileContext compiling, CompoundTerm source) {
        //
        // Optimizes selection of unifier depending on if one or both terms are variables, structures and/or atomic.
        //
        Term left = source.get(0);
        Term right = source.get(1);

        if (!left.isInstantiated()) {
            compiling.add(source, new ExecUnifyInstantiate((Variable) left, right));
            return;
        }
        if (!right.isInstantiated()) {
            // prefer variable on 'left'
            compiling.add(source, new ExecUnifyInstantiate((Variable) right, left));
            return;
        }
        if (left.isGrounded() && right.isGrounded()) {
            // if left and right are both grounded, just compare right now
            if (left.compareTo(right) == 0) {
                // Control.TRUE
                // TODO should this be a warning at least?
            } else {
                // TODO should this be a warning at least?
                compiling.add(source, Control.FALSE);
            }
            return;
        }
        if (left.isGrounded() && !right.isGrounded()) {
            // prefer templates to left (compiled unifier will create entries for the variables)
            Term swap = left;
            left = right;
            right = swap;
        }
        if (left instanceof CompoundTerm && right instanceof CompoundTerm) {
            // unify compound terms (including lists)
            compiling.add(source, new ExecUnifyCompounds((CompoundTerm) left, (CompoundTerm) right));
            return;
        }
        // any other combination never unifies
        // TODO should this be a warning at least?
        compiling.add(source, Control.FALSE);
    }

    /**
     * Success if left and right fails to unify. Fails (with no unification) if they can unify.
     *
     * @param compiling Compiling context
     * @param source Left/Right terms to unify.
     */
    @Predicate(value = "\\=", arity = 2)
    public static void notUnify(CompileContext compiling, CompoundTerm source) {
        CompileContext nested = compiling.newContext();
        unify(nested, source);
        compiling.add(source,
                new ExecIfThenElse(
                        nested.toInstruction(),
                        Control.FALSE,
                        Control.TRUE));
    }

    /**
     * Instantiate variable from expression
     *
     * @param compiling Compilation context
     * @param source      The 'is' term
     */
    @Predicate(value = "is", arity = 2)
    public static void is(CompileContext compiling, CompoundTerm source) {
        Term left = source.get(0);
        Term right = source.get(1);
        CompileMathExpression expr = new CompileMathExpression(compiling).compile(right);
        compiling.add(source, new ExecIs(expr, left));
    }

}
