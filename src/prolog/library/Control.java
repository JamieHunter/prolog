// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Interned;
import prolog.bootstrap.Predicate;
import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.instructions.ExecBlock;
import prolog.instructions.ExecCall;
import prolog.instructions.ExecDisjunction;
import prolog.instructions.ExecIfThenElse;
import prolog.instructions.ExecOnce;
import prolog.instructions.ExecRepeat;

import java.util.ArrayList;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Bootstraps control predicates.
 */
public final class Control {
    private Control() {
        // Static methods/fields only
    }

    /**
     * True is actually a no-op. When compiling, just don't compile the instruction.
     */
    @Predicate(value = "true", arity = 0)
    public static void truePredicate(CompileContext compiling, CompoundTerm trueTerm) {
        // compile nothing!
        // The block being compiled by compiling will replace the block with TRUE if the TRUE instruction is needed.
    }

    /**
     * "Unoptimized" singleton true, returned by {@link prolog.instructions.ExecBlock#from(ArrayList)} if needed.
     */
    public static final Instruction TRUE = environment -> {
    };

    /**
     * False is a singleton instruction
     */
    @Predicate({"false", "fail"})
    public static final Instruction FALSE = Environment::backtrack;

    /**
     * Repeat is a singleton instruction with decision point
     */
    @Predicate("repeat")
    public static final Instruction REPEAT = ExecRepeat.REPEAT;

    /**
     * When cut is executed, it trims all decision points within the current cut cutScope,
     * and sets mode to indicate this is now deterministic
     */
    @Predicate("!")
    public static final Instruction CUT = Environment::cutDecisionPoints;

    /**
     * Parse a ',' tree into a single conjunction (block) entry.
     *
     * @param compiling Compilation context
     * @param term      Initial ',' term
     */
    @Predicate(value = ",", arity = 2)
    public static void conjunction(CompileContext compiling, CompoundTerm term) {
        Term iter = term;
        // Note that ','(','(a,b),','(c,d)) will do the right thing here
        // compiling into block {a,b,c,d)
        while (CompoundTerm.termIsA(iter, Interned.COMMA_FUNCTOR, 2)) {
            Term blockTerm = ((CompoundTerm) iter).get(0);
            blockTerm.compile(compiling);
            iter = ((CompoundTerm) iter).get(1);
        }
        iter.compile(compiling);
    }

    /**
     * IF-THEN Construct.
     * @param compiling Compiling context
     * @param term Initial '-&gt;' term.
     */
    @Predicate(value = "->", arity = 2)
    public static void ifThen(CompileContext compiling, CompoundTerm term) {
        // IF-THEN construct, see IF-THEN-ELSE construct
        compiling.add(new ExecIfThenElse(
                compiling.environment(),
                term.get(0),
                ExecBlock.from(compiling.environment(), term.get(1)),
                Control.FALSE));
    }

    /**
     * Either (a) Disjunction, or (b) IF-THEN-ELSE construct.
     * Nested ';' are parsed into a single disjunction entry.
     *
     * @param compiling Compilation context
     * @param term      Initial ';' term
     */
    @Predicate(value = ";", arity = 2)
    public static void disjunction(CompileContext compiling, CompoundTerm term) {
        Environment environment = compiling.environment();
        ArrayList<Instruction> alternates = new ArrayList<>();
        if (CompoundTerm.termIsA(term.get(0), Interned.IF_FUNCTOR, 2)) {
            // IF-THEN-ELSE construct
            // Note that the meaning of ';' here is different to ';' outside of this construct.
            // To understand that, consider the term ((A -> B ; C) ; D)
            // if A fails, C is executed, if C fails, D is executed.
            // if A succeeds, B is executed. if B fails, D is executed, but C is not.
            // In neither case is A re-executed, there is an implicit (once(A)).
            // Note that (A -> B ; C ; D) is the same as (A -> B; (C ; D)) not ((A -> B ; C); D)
            // but should be avoided anyway.
            CompoundTerm ifThen = (CompoundTerm) term.get(0);
            // Cond -> Then ; Else
            compiling.add(new ExecIfThenElse(
                    environment,
                    ifThen.get(0), // Cond
                    ExecBlock.from(compiling.environment(), ifThen.get(1)), // Then
                    ExecBlock.from(compiling.environment(), term.get(1)))); // Else
        } else {
            // Disjunction construct
            Term iter = term;
            while (CompoundTerm.termIsA(iter, Interned.SEMICOLON_FUNCTOR, 2)) {
                Term blockTerm = ((CompoundTerm) iter).get(0);
                alternates.add(ExecBlock.from(environment, blockTerm));
                iter = ((CompoundTerm) iter).get(1);
            }
            alternates.add(ExecBlock.from(environment, iter));
            compiling.add(new ExecDisjunction(alternates.toArray(new Instruction[alternates.size()])));
        }
    }

    /**
     * Call is a scoped execution block.
     *
     * @param compiling Compiling context
     * @param term      Call term.
     */
    @Predicate(value = "call", arity = 1)
    public static void call(CompileContext compiling, CompoundTerm term) {
        Term callTerm = term.get(0);
        // while p1,call(p2),p3 may seem the same as p1,p2,p3,
        // cut behavior is modified by the call
        compiling.add(new ExecCall(compiling.environment(), callTerm));
    }

    /**
     * Not provable.
     *
     * @param compiling Compilation context
     * @param term      The predicate to call
     */
    @Predicate(value = {"\\+", "not"}, arity = 1)
    public static void notProvable(CompileContext compiling, CompoundTerm term) {
        Term callTerm = term.get(0);
        final Environment environment = compiling.environment();
        compiling.add(
                new ExecIfThenElse(
                        environment,
                        callTerm,
                        FALSE,
                        TRUE));

    }

    /**
     * Variation of call with an implied cut immediately after a successful call
     *
     * @param compiling Compilation context
     * @param term      The predicate to call
     */
    @Predicate(value = "once", arity = 1)
    public static void once(CompileContext compiling, CompoundTerm term) {
        Term callTerm = term.get(0);
        compiling.add(new ExecOnce(compiling.environment(), callTerm));
    }
}
