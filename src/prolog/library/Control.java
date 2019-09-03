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
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;
import prolog.instructions.ComposedCallInstruction;
import prolog.instructions.CurriedCallInstruction;
import prolog.instructions.DeferredCallInstruction;
import prolog.instructions.ExecBagOf;
import prolog.instructions.ExecBlock;
import prolog.instructions.ExecCall;
import prolog.instructions.ExecDisjunction;
import prolog.instructions.ExecFindAll;
import prolog.instructions.ExecIfThenElse;
import prolog.instructions.ExecIgnore;
import prolog.instructions.ExecOnce;
import prolog.instructions.ExecRepeat;
import prolog.instructions.ExecSetOf;

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
    @Predicate(value = "true", arity = 0, notrace = true)
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
    @Predicate(value = {"false", "fail"}, notrace = true)
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
    @Predicate(value = "!", notrace = true)
    public static final Instruction CUT = Environment::cutDecisionPoints;

    /**
     * Parse a ',' tree into a single conjunction (block) entry.
     *
     * @param compiling Compilation context
     * @param source      Initial ',' term
     */
    @Predicate(value = ",", arity = 2, notrace = true)
    public static void conjunction(CompileContext compiling, CompoundTerm source) {
        Term iter = source;
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
     *
     * @param compiling Compiling context
     * @param source      Initial '-&gt;' term.
     */
    @Predicate(value = "->", arity = 2, notrace = true)
    public static void ifThen(CompileContext compiling, CompoundTerm source) {
        // IF-THEN construct, see IF-THEN-ELSE construct
        compiling.add(source, new ExecIfThenElse(
                ExecBlock.nested(compiling, source.get(0)), // or deferred?
                ExecBlock.nested(compiling, source.get(1)),
                Control.FALSE));
    }

    /**
     * Either (a) Disjunction, or (b) IF-THEN-ELSE construct.
     * Nested ';' are parsed into a single disjunction entry.
     *
     * @param compiling Compilation context
     * @param source      Initial ';' term
     */
    @Predicate(value = ";", arity = 2, notrace = true)
    public static void disjunction(CompileContext compiling, CompoundTerm source) {
        ArrayList<Instruction> alternates = new ArrayList<>();
        if (CompoundTerm.termIsA(source.get(0), Interned.IF_FUNCTOR, 2)) {
            // IF-THEN-ELSE construct
            // Note that the meaning of ';' here is different to ';' outside of this construct.
            // To understand that, consider the term ((A -> B ; C) ; D)
            // if A fails, C is executed, if C fails, D is executed.
            // if A succeeds, B is executed. if B fails, D is executed, but C is not.
            // In neither case is A re-executed, there is an implicit (once(A)).
            // Note that (A -> B ; C ; D) is the same as (A -> B; (C ; D)) not ((A -> B ; C); D)
            // but should be avoided anyway.
            CompoundTerm ifThen = (CompoundTerm) source.get(0);
            // Cond -> Then ; Else
            compiling.add(source, new ExecIfThenElse(
                    ExecBlock.nested(compiling, ifThen.get(0)), // Cond (or deferred?)
                    ExecBlock.nested(compiling, ifThen.get(1)), // Then
                    ExecBlock.nested(compiling, source.get(1)))); // Else
        } else {
            // Disjunction construct
            Term iter = source;
            while (CompoundTerm.termIsA(iter, Interned.SEMICOLON_FUNCTOR, 2)) {
                Term blockTerm = ((CompoundTerm) iter).get(0);
                alternates.add(ExecBlock.nested(compiling, blockTerm));
                iter = ((CompoundTerm) iter).get(1);
            }
            alternates.add(ExecBlock.nested(compiling, iter));
            compiling.add(source, new ExecDisjunction(alternates.toArray(new Instruction[alternates.size()])));
        }
    }

    /**
     * Call is a scoped execution block.
     *
     * @param compiling Compiling context
     * @param source      Call term.
     */
    @Predicate(value = "call", arity = 1)
    public static void call(CompileContext compiling, CompoundTerm source) {
        Term callTerm = source.get(0);
        // while p1,call(p2),p3 may seem the same as p1,p2,p3,
        // cut behavior is modified by the call
        // note that compilation of the call is deferred further, so any error only occurs if actually trying
        // to call.
        compiling.add(source, new ExecCall(ExecBlock.deferred(callTerm)));
    }

    /**
     * Apply is similar to call, but appends a list of arguments to the goal
     *
     * @param compiling Compiling context
     * @param source      Call term.
     */
    @Predicate(value = "apply", arity = 2)
    public static void apply(CompileContext compiling, CompoundTerm source) {
        Term callTerm = source.get(0);
        Term argTerm = source.get(1);
        // This is similar to call/2 but the parameters passed as a list, handling of both
        // get deferred.
        compiling.add(source, new ExecCall(new CurriedCallInstruction(callTerm, argTerm)));
    }

    /**
     * Cross between call and apply
     *
     * @param compiling Compiling context
     * @param source      Call term.
     */
    @Predicate(value = "call", arity = 2, vararg = true)
    public static void call2(CompileContext compiling, CompoundTerm source) {
        Term callTerm = source.get(0);
        Term[] members = new Term[source.arity()-1];
        for (int i = 1; i < source.arity(); i++) {
            members[i-1] = source.get(i);
        }
        compiling.add(source, new ExecCall(new ComposedCallInstruction(callTerm, members)));
    }

    /**
     * Not provable.
     *
     * @param compiling Compilation context
     * @param source      The predicate to call
     */
    @Predicate(value = {"\\+", "not"}, arity = 1, notrace = true)
    public static void notProvable(CompileContext compiling, CompoundTerm source) {
        Term callTerm = source.get(0);
        compiling.add(source,
                new ExecIfThenElse(
                        ExecBlock.nested(compiling, callTerm),
                        FALSE,
                        TRUE));

    }

    /**
     * Variation of call with an implied cut immediately after a successful call
     *
     * @param compiling Compilation context
     * @param source      The predicate to call
     */
    @Predicate(value = "once", arity = 1)
    public static void once(CompileContext compiling, CompoundTerm source) {
        Term callTerm = source.get(0);
        compiling.add(source, new ExecOnce(ExecBlock.nested(compiling, callTerm)));
    }

    /**
     * Variation of call but always succeeds
     *
     * @param compiling Compilation context
     * @param source      The predicate to call
     */
    @Predicate(value = "ignore", arity = 1)
    public static void ignore(CompileContext compiling, CompoundTerm source) {
        Term callTerm = source.get(0);
        compiling.add(source, new ExecIgnore(ExecBlock.nested(compiling, callTerm)));
    }

    /**
     * Given findall(Term, Goal, List), with Term being a term containing one or more variables,
     * recursively iterate (with backtracking) Goal to enumerate all possible
     * solutions. At the end of each solution, Term is copied and appended to a list. Term is expected to
     * contain one or more variables specified in Goal. Once the set of solutions have been exhausted,
     * the list is unified with List.
     *
     * @param compiling Compiling context
     * @param source      Contains the arguments
     */
    @Predicate(value = "findall", arity = 3)
    public static void findall(CompileContext compiling, CompoundTerm source) {
        Term template = source.get(0);
        Term callable = source.get(1);
        Term list = source.get(2);
        compiling.add(source, new ExecFindAll(template, callable, list));
    }

    /**
     * Builds on the functionality of findall.
     * Given bagof(Term, Goal, List), the entire solution set of findall is first determined. The solutions
     * are re-collated to produce one or more solutions using the free variables of Goal. The syntax of
     * Var^Goal or more generically Term^Goal is used to introduce additional existential variables such that if
     * there are no free variables, bagof degenerates to be almost the same as findall.
     *
     * @param compiling Compiling context
     * @param source      Contains the arguments
     */
    @Predicate(value = "bagof", arity = 3)
    public static void bagof(CompileContext compiling, CompoundTerm source) {
        Term variable = source.get(0);
        Term callable = source.get(1);
        Term list = source.get(2);
        compiling.add(source, new ExecBagOf(variable, callable, list));
    }

    /**
     * Builds on the functionality of bagof.
     * Given setof(Term, Goal, List), Each List produced is further collated - sorted and deduplicated.
     *
     * @param compiling Compiling context
     * @param source      Contains the arguments
     */
    @Predicate(value = "setof", arity = 3)
    public static void setof(CompileContext compiling, CompoundTerm source) {
        Term variable = source.get(0);
        Term callable = source.get(1);
        Term list = source.get(2);
        compiling.add(source, new ExecSetOf(variable, callable, list));
    }
}
