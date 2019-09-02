// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Predicate;
import prolog.execution.CompileContext;
import prolog.execution.Instruction;
import prolog.execution.LocalContext;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.functions.CompileMathExpression;
import prolog.instructions.ExecIs;
import prolog.instructions.ExecUnifyCompounds;
import prolog.instructions.ExecUnifyInstantiate;
import prolog.unification.Unifier;
import prolog.variables.Variable;

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
            compiling.add(new ExecUnifyInstantiate(source, (Variable) left, right));
            return;
        }
        if (!right.isInstantiated()) {
            // prefer variable on 'left'
            compiling.add(new ExecUnifyInstantiate(source, (Variable) right, left));
            return;
        }
        if (left.isGrounded() && right.isGrounded()) {
            // if left and right are both grounded, just compare right now
            if (left.compareTo(right) == 0) {
                // compiling.add(Control.TRUE); - not needed
                // TODO should this be a warning at least?
            } else {
                // TODO should this be a warning at least?
                compiling.add(Control.FALSE);
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
            compiling.add(new ExecUnifyCompounds(source, (CompoundTerm) left, (CompoundTerm) right));
            return;
        }
        // any other combination never unifies
        // TODO should this be a warning at least?
        compiling.add(Control.FALSE);
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
        compiling.add(new ExecIs(source, expr, left));
    }

}
