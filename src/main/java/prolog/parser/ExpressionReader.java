// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.parser;

import prolog.bootstrap.Interned;
import prolog.bootstrap.Operators;
import prolog.constants.Atomic;
import prolog.constants.PrologAtom;
import prolog.constants.PrologAtomLike;
import prolog.constants.PrologEOF;
import prolog.constants.PrologEmptyList;
import prolog.constants.PrologNumber;
import prolog.constants.PrologQuotedAtom;
import prolog.enumerators.SimplifyTerm;
import prolog.exceptions.PrologSyntaxError;
import prolog.execution.Environment;
import prolog.execution.OperatorEntry;
import prolog.expressions.BracketedTerm;
import prolog.expressions.CompoundTerm;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;
import prolog.expressions.TermList;
import prolog.expressions.TermListImpl;
import prolog.flags.ReadOptions;
import prolog.unification.Unifier;
import prolog.variables.UnboundVariable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/**
 * Given a stream of tokens (expression), produce a Prolog term obeying operators.
 */
public final class ExpressionReader {
    private final Tokenizer tokenizer;
    private final ReadOptions options;
    private final Environment environment;

    // Stack of operators
    private final LinkedList<OperatorEntry> operators = new LinkedList<>();
    // Stack of arguments (operator symbols are also added to this stack)
    private final LinkedList<Term> stack = new LinkedList<>();
    // Current state
    private State state = State.ARG_OR_PREFIX;

    /**
     * Expression evaluation states
     */
    private enum State {
        ARG_OR_PREFIX,
        OPERATOR
    }

    /**
     * Create new expression reader. This may be used to read multiple sentences.
     *
     * @param tokenizer Tokenizer
     */
    public ExpressionReader(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.options = tokenizer.options();
        this.environment = tokenizer.environment();
    }

    /**
     * Read and parse an expression, where expression is expected to end with the term '.'.
     *
     * @return Resulting term of expression
     */
    public Term read() {
        boolean assumedAtEof = options.fullStop == ReadOptions.FullStop.ATOM_optional;
        Term t = read(Interned.DOT, () -> !tokenizer.isNext('('), tokenizer.nextToken(), assumedAtEof);
        tokenizer.skipEOLN();
        Term reduced = t.enumTerm(new SimplifyTerm(environment));
        if (options.variables.isPresent()) {
            Term variables = collectVariables(tokenizer.getVariableMap());
            if (!Unifier.unify(environment.getLocalContext(), options.variables.get(), variables)) {
                environment.backtrack();
            }
        }
        if (options.variableNames.isPresent()) {
            Term variables = collectVariableNames(tokenizer.getVariableMap());
            if (!Unifier.unify(environment.getLocalContext(), options.variableNames.get(), variables)) {
                environment.backtrack();
            }
        }
        return reduced;
    }

    /**
     * Variables by reference only.
     *
     * @param varMap Map of variables
     * @return List
     */
    private Term collectVariables(Map<String, UnboundVariable> varMap) {
        List<Term> termList = new ArrayList<>();
        termList.addAll(varMap.values());
        return TermList.from(termList);
    }

    /**
     * Variables by name=reference.
     *
     * @param varMap Map of variables
     * @return List
     */
    private Term collectVariableNames(Map<String, UnboundVariable> varMap) {
        List<Term> termList = varMap.values().stream().map(
                v -> new CompoundTermImpl(Interned.EQUALS_FUNCTOR,
                        new PrologAtom(v.name()),
                        v)).collect(Collectors.toList());
        return TermList.from(termList);
    }

    /**
     * Read and parse a term expression. Terminal term is specified, and depends on context. This version
     * is called if initial token is known.
     *
     * @param terminal        Atom that indicates end of parse
     * @param confirmTerminal Called to make sure terminal really is a terminal
     * @param initialToken    Initial token.
     * @param assumedAtEoF    If true, terminal is optional at end of file
     * @return Resulting term of expression
     */
    private Term read(PrologAtomLike terminal, BooleanSupplier confirmTerminal, Term initialToken, boolean assumedAtEoF) {
        operators.push(OperatorEntry.TERMINAL); // adds a guard for handleEnd
        State oldState = state;
        try {
            state = State.ARG_OR_PREFIX;
            for (Term token = initialToken; token != PrologEOF.EOF; token = tokenizer.nextToken()) {
                if (is(token, Interned.OPEN_BRACKET)) {
                    if (state == State.OPERATOR) {
                        handleStructure();
                    } else {
                        handleBracketTerm();
                    }
                } else if (is(token, Interned.OPEN_SQUARE_BRACKET)) {
                    handleList();
                } else if (is(token, Interned.OPEN_BRACES)) {
                    handleBracesTerm();
                } else if (is(token, terminal) && confirmTerminal.getAsBoolean()) {
                    return handleEnd(terminal);
                } else {
                    handleTerm(token, !tokenizer.isNext('('));
                }
            }
            // EOF
            if (state == State.ARG_OR_PREFIX && operators.peek() == OperatorEntry.TERMINAL) {
                // if no terms read, this is an acceptable place to receive an end-of-file
                operators.pop();
                return PrologEOF.EOF;
            } else if (assumedAtEoF) {
                // If we can assume terminal at end of file, then handle as if terminal was given. Note the ordering
                // here so that EOF is returned when it is appropriate.
                return handleEnd(terminal);
            } else {
                throw PrologSyntaxError.eofError(environment,
                        "EOF reached before term was completed, missing '" + terminal + "' ?");
            }
        } finally {
            state = oldState;
        }
    }

    /**
     * Utility method - determine if term on left is the atom on the right - as this is used for syntactic elements,
     * quoted atoms are excluded.
     *
     * @param term  Term under test
     * @param other Atom to compare with
     * @return true if term is the given atom
     */
    private boolean is(Term term, PrologAtomLike other) {
        return term.isAtom() && term.compareTo(other) == 0 &&
                !(term instanceof PrologQuotedAtom);
    }

    /**
     * Utility method, determine if term on left is unnecessarily quoted
     *
     * @param term Term under test
     * @return true if unnecessarily quoted
     */
    private boolean isOverquotedAtom(Term term) {
        if (term instanceof PrologQuotedAtom) {
            // atom was read as quoted, return true if it didn't need to be quoted
            return !((PrologQuotedAtom) term).needsQuoting();
        } else {
            // atom was not read as quoted, so doesn't matter
            return false;
        }
    }

    /**
     * Use Prolog's dynamic precedence to rewrite (a,b,c,...) in a manner that can interpret the list as a comma in
     * the right context. This also wraps a single term to identify that a layer of brackets have been applied.
     * BracketedTerm's are eliminated during the simplification step.
     *
     * @param term Term, assumed to be a comma-list structure
     * @return a BracketedTerm
     */
    private BracketedTerm rewriteBracketedCommas(Term term) {
        ArrayList<Term> list = new ArrayList<>();
        while (CompoundTerm.termIsA(term, Interned.COMMA_FUNCTOR, 2) && !(term instanceof BracketedTerm)) {
            CompoundTerm commaTerm = (CompoundTerm) term;
            list.add(commaTerm.get(0).value(environment));
            term = commaTerm.get(1);
        }
        list.add(term.value(environment)); // last term
        return new BracketedTerm(list);
    }

    /**
     * Use Prolog's dynamic precedence to interpret a comma-separated list within '[' and ']'. Commas are handled
     * using {@link #rewriteBracketedCommas(Term)}. Additionally '|' is handled here.
     *
     * @param term Term, assumed to be a compound term
     * @return a compact list, or empty list
     */
    private Term convertList(Term term) {
        Term tailTerm = PrologEmptyList.EMPTY_LIST;
        if (CompoundTerm.termIsA(term, Interned.BAR_FUNCTOR, 2)) {
            CompoundTerm barTerm = (CompoundTerm) term;
            tailTerm = barTerm.get(1).value(environment);
            term = barTerm.get(0);
        }
        BracketedTerm bracketedTerm = rewriteBracketedCommas(term);
        return new TermListImpl(bracketedTerm.get(), tailTerm);
    }

    /**
     * Atom on stack, '(' parsed and assumed to be a compound term.
     */
    private void handleStructure() {
        Term atom = stack.pop();
        if (atom instanceof BracketedTerm) {
            // unwrap one level of brackets if it consists of a single term
            // This allows (+)(1,2) where +(1,2) would do the wrong thing.
            BracketedTerm bracketed = (BracketedTerm) atom;
            List<Term> list = bracketed.get();
            if (list.size() == 1) {
                atom = list.get(0);
            }
        }
        if (!atom.isAtom()) {
            throw PrologSyntaxError.functorError(environment, "Functor expected before '('");
        }
        Term result = read(Interned.CLOSE_BRACKET, () -> true, tokenizer.nextToken(), false);
        BracketedTerm bracketed = rewriteBracketedCommas(result);
        // replace previous term
        stack.push(new CompoundTermImpl((Atomic) atom, bracketed.get()));
    }

    /**
     * Treat the contents of '(' and ')' as term. Note that commas are perfectly legal
     * in this context. The term is wrapped inside BracketedTerm to protect it from being
     * mis-interpreted when used as an argument where commas have special meaning.
     */
    private void handleBracketTerm() {
        Term result = read(Interned.CLOSE_BRACKET, () -> true, tokenizer.nextToken(), false);
        handleTerm(rewriteBracketedCommas(result), false);
    }

    /**
     * Handle content of '{' ... '}' as a single term
     */
    private void handleBracesTerm() {
        Term next = tokenizer.nextToken();
        if (is(next, Interned.CLOSE_BRACES)) {
            handleTerm(Interned.EMPTY_BRACES_ATOM, false);
        } else {
            Term nestedTerm = read(Interned.CLOSE_BRACES, () -> true, next, false);
            handleTerm(nestedTerm, false);
        }
    }

    /**
     * Handle content of '[' ... ']'
     */

    private void handleList() {
        Term next = tokenizer.nextToken();
        if (is(next, Interned.CLOSE_SQUARE_BRACKET)) {
            handleTerm(PrologEmptyList.EMPTY_LIST, false);
        } else {
            Term nestedTerm = read(Interned.CLOSE_SQUARE_BRACKET, () -> true, next, false);
            handleTerm(convertList(nestedTerm), false);
        }
    }

    /**
     * Handle self-identifying term, constant, operator, etc
     *
     * @param term                  Self identifying term
     * @param allowAsPrefixOperator true if term is permitted to be a prefix operator, false otherwise
     */
    private void handleTerm(Term term, boolean allowAsPrefixOperator) {
        OperatorEntry op1 = OperatorEntry.ARGUMENT; // prefix operator if it exists
        OperatorEntry op2 = OperatorEntry.ARGUMENT; // infix/postfix operator if it exists
        if (term.isAtom() && !isOverquotedAtom(term)) {
            op2 = environment.getInfixPostfixOperator((Atomic) term);
            if (state == State.ARG_OR_PREFIX && allowAsPrefixOperator) {
                op1 = environment.getPrefixOperator((Atomic) term);
            }
        }
        if (state == State.ARG_OR_PREFIX && op2 != OperatorEntry.ARGUMENT && op1 == OperatorEntry.ARGUMENT) {
            // consider if last operator was interpreted incorrectly
            OperatorEntry lastOp = operators.peek();
            if (lastOp.getCode().isPrefix() &&
                    (op2 == Operators.COMMA || lastOp.getPrecedence() < op2.getPrecedence())) {
                // re-interpret last operator as non-operator
                operators.pop();
                state = State.OPERATOR;
            }
        }
        if (state == State.OPERATOR && op2 == OperatorEntry.ARGUMENT) {
            // Argument given at infix/postfix operator position - this is clearly an error
            throw PrologSyntaxError.expectedOperatorError(environment, "Unexpected term " + term + " where operator is expected");
        }
        if (state == State.ARG_OR_PREFIX && op1 == OperatorEntry.ARGUMENT) {
            // Argument given in arg/prefix position
            // Given o1 x o2 y and at position x,
            // We cannot differentiate between
            // ((o1 x) o2 y) and
            // (o1 (x o2 y))
            // until we read more localContext
            stack.push(term);
            // only permit binary or postfix operators
            state = State.OPERATOR;
            return;
        }

        if (state == State.ARG_OR_PREFIX) {
            // Operator op1 is valid
            operators.push(op1);
            stack.push(term); // save the operator for unrolling
            // keep processing args and prefix operators
            return;
        }

        //
        // Only binary and postfix operators remain, reduction can now occur
        //
        while (operators.peek() != OperatorEntry.TERMINAL) {
            OperatorEntry lastOp = operators.pop();
            int compare = lastOp.compareTo(op2);
            if (compare == 0) {
                // cannot resolve left/right
                throw new UnsupportedOperationException("NYI");
            }
            if (compare > 0) {
                // right associative, done processing
                operators.push(lastOp);
                break;
            }
            reduceOp(lastOp);
        }
        if (op2.getCode().isPostfix()) {
            // postfix operators are reduced immediately
            Atomic atom = (Atomic) term;
            Term theArg = stack.pop();
            stack.push(new CompoundTermImpl(atom, theArg));
            state = State.OPERATOR; // still looking for operator
        } else {
            // infix operators have to wait until more parsing is done
            operators.push(op2);
            stack.push(term); // save the infix operator
            state = State.ARG_OR_PREFIX; // expect argument next
        }
    }

    /**
     * Apply operator reduction on stack.
     *
     * @param op Target operator
     */
    private void reduceOp(OperatorEntry op) {
        if (op.getCode().isPrefix()) {
            Term theArg = stack.pop();
            Atomic atom = (Atomic) stack.pop();
            // +const or -const handled immediately
            if (is(atom, Interned.MINUS_ATOM) && theArg.isNumber()) {
                // negate immediately
                theArg = ((PrologNumber) theArg).negate();
                stack.push(theArg);
            } else if (is(atom, Interned.PLUS_ATOM) && theArg.isNumber()) {
                // eliminate +
                stack.push(theArg);
            } else {
                stack.push(new CompoundTermImpl(atom, theArg));
            }
        } else {
            Term rightArg = stack.pop();
            Atomic atom = (Atomic) stack.pop();
            Term leftArg = stack.pop();
            stack.push(new CompoundTermImpl(atom, leftArg, rightArg));
        }
    }

    /**
     * Reconsider last operator parsed as being an argument instead.
     * Case (1) - (op) -- op prefix, becomes (arg).
     * Case (2) - (arg op), op infix, is illegal
     *
     * @param terminal Expected terminal
     */
    private void reinterpretLastOperator(PrologAtomLike terminal) {
        // consider (atom) scenario where atom was being interpreted as a prefix
        if (operators.peek() != OperatorEntry.TERMINAL) {
            OperatorEntry lastOp = operators.pop();
            if (lastOp.getCode().isPrefix()) {
                return; // dropping operator is sufficient, prefix is now treated as arg
            }
            // don't need to put back lastOp, we're going to fail. Lets get the operator to
            // give a more meaningful error.
            Atomic op = (Atomic) stack.pop();
            throw PrologSyntaxError.expectedArgumentError(environment,
                    "Expected argument after '" + op.toString() + "'");
        } else {
            // initial position
            if (is(terminal, Interned.DOT)) {
                throw PrologSyntaxError.expectedSentenceError(environment, "Expected sentence");
            } else {
                throw PrologSyntaxError.expectedArgumentError(environment, "Expected arg");
            }
        }
    }

    /**
     * Handle end of expression, reduce all remaining operators
     *
     * @param terminal Expected terminal
     * @return Completed term
     */
    private Term handleEnd(PrologAtomLike terminal) {
        if (state == State.ARG_OR_PREFIX) {
            reinterpretLastOperator(terminal);
        }

        //
        // now unconditionally reduce all the operators
        // processing all remaining right-associative and unary operators
        //
        while (operators.peek() != OperatorEntry.TERMINAL) {
            OperatorEntry lastOp = operators.pop();
            reduceOp(lastOp);
        }
        operators.pop(); // terminal
        return stack.pop(); // should be a single item of data
    }

}
