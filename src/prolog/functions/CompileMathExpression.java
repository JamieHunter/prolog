// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.functions;

import prolog.bootstrap.Interned;
import prolog.constants.PrologAtomLike;
import prolog.constants.PrologNumber;
import prolog.exceptions.FutureTypeError;
import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.instructions.ExecPushConstant;
import prolog.instructions.ExecPushNumberVariable;
import prolog.instructions.ExecSimpleBlock;
import prolog.predicates.Predication;
import prolog.variables.Variable;

import java.util.ArrayList;

/**
 * Compile a series of operations based on interpreting a term as an evaluable expression.
 */
public class CompileMathExpression {

    private final ArrayList<Instruction> compiling = new ArrayList<>();
    private final Environment.Shared environmentShared;

    /**
     * Construct a math expression builder
     *
     * @param compiling Parent compiling context.
     */
    public CompileMathExpression(CompileContext compiling) {
        this.environmentShared = compiling.environmentShared();
    }

    /**
     * Compile the term as an expression. No precedence handling is required here as that was already done by the
     * Prolog reader.
     *
     * @param term Expression as a term.
     */
    public CompileMathExpression compile(Term term) {
        if (term.isAtomic()) {
            if (term.isAtom()) {
                // per standard
                throw new FutureTypeError(Interned.EVALUABLE_TYPE, new Predication((PrologAtomLike) term, 0).term());
            }
            if (!term.isNumber()) {
                // per standard
                throw new FutureTypeError(Interned.NUMBER_TYPE, term);
            }
            // number constants go onto stack
            compiling.add(new ExecPushConstant((PrologNumber) term));
            return this;
        }
        if (term instanceof Variable) {
            // variables need to be resolved and checked as they are executed
            compiling.add(new ExecPushNumberVariable((Variable) term));
            return this;
        }
        // handle operators/functions
        if (term instanceof CompoundTerm) {
            CompoundTerm compound = (CompoundTerm) term;
            Predication predication = compound.toPredication();
            StackFunction func = environmentShared.lookupFunction(predication);
            if (func != null) {
                compileFunction(compound, func);
                return this;
            }
            throw new FutureTypeError(Interned.EVALUABLE_TYPE, predication.term());
        }
        throw new FutureTypeError(Interned.NUMBER_TYPE, term);
    }

    /**
     * Compile the compound term by compiling all the arguments followed by the function.
     *
     * @param term     Compound term representing function and arguments.
     * @param function Function associated with term.
     */
    public CompileMathExpression compileFunction(CompoundTerm term, StackFunction function) {
        for (int i = 0; i < term.arity(); i++) {
            compile(term.get(i));
        }
        function.compile(compiling);
        return this;
    }

    /**
     * @return array of stack-based instructions.
     */
    public Instruction toInstruction() {
        return ExecSimpleBlock.from(compiling);
    }
}
