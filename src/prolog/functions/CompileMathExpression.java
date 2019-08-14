// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.functions;

import prolog.constants.Atomic;
import prolog.constants.PrologNumber;
import prolog.exceptions.PrologTypeError;
import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.instructions.ExecPushConstant;
import prolog.instructions.ExecPushNumberVariable;
import prolog.predicates.Predication;
import prolog.variables.Variable;

/**
 * Compile a series of operations based on interpreting a term as an evaluable expression.
 */
public class CompileMathExpression {

    private final CompileContext compiling;
    private final Environment environment;

    /**
     * Construct a math expression builder associated with a compile context.
     *
     * @param compiling Compiling context.
     */
    public CompileMathExpression(CompileContext compiling) {
        this.compiling = compiling;
        this.environment = compiling.environment();
    }

    /**
     * Compile the term as an expression. No precedence handling is required here as that was already done by the
     * Prolog reader.
     *
     * @param term Expression as a term.
     */
    public void compile(Term term) {
        if (term.isAtomic()) {
            if (term.isAtom()) {
                // per standard
                throw PrologTypeError.evaluableExpected(environment, new Predication((Atomic)term, 0).term());
            }
            if (!term.isNumber()) {
                // per standard
                throw PrologTypeError.numberExpected(environment, term);
            }
            // number constants go onto stack
            compiling.add(new ExecPushConstant((PrologNumber) term));
            return;
        }
        if (term instanceof Variable) {
            // variables need to be resolved and checked as they are executed
            compiling.add(new ExecPushNumberVariable((Variable) term));
            return;
        }
        // handle operators/functions
        if (term instanceof CompoundTerm) {
            CompoundTerm compound = (CompoundTerm) term;
            Predication predication = new Predication(compound.functor(), compound.arity());
            StackFunction func = environment.lookupFunction(predication);
            if (func != null) {
                compileFunction(compound, func);
                return;
            }
            throw PrologTypeError.evaluableExpected(environment, predication.term());
        }
        throw PrologTypeError.numberExpected(environment, term);
    }

    /**
     * Compile the compound term by compiling all the arguments followed by the function.
     *
     * @param term     Compound term representing function and arguments.
     * @param function Function associated with term.
     */
    public void compileFunction(CompoundTerm term, StackFunction function) {
        for (int i = 0; i < term.arity(); i++) {
            compile(term.get(i));
        }
        function.compile(compiling);
    }
}
