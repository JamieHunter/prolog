// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.parser;

import prolog.bootstrap.Interned;
import prolog.bootstrap.Operators;
import prolog.constants.Atomic;
import prolog.constants.PrologAtom;
import prolog.constants.PrologEOF;
import prolog.constants.PrologEmptyList;
import prolog.constants.PrologNumber;
import prolog.exceptions.PrologSyntaxError;
import prolog.execution.CopyTermContext;
import prolog.execution.Environment;
import prolog.execution.OperatorEntry;
import prolog.expressions.BracketedTerm;
import prolog.expressions.CompoundTerm;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;
import prolog.expressions.TermListImpl;
import prolog.flags.ReadOptions;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BooleanSupplier;

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
        Term t = read(Interned.DOT, () -> !tokenizer.isNext('('), tokenizer.nextToken());
        tokenizer.skipEOLN();
        return t.copyTerm(new CopyTermContext.KeepVars(environment));
    }

    /**
     * Read and parse a term expression. Terminal term is specified, and depends on context. This version
     * is called if initial token is known.
     *
     * @param terminal     Atom that indicates end of parse
     * @param initialToken Initial token.
     * @return Resulting term of expression
     */
    private Term read(PrologAtom terminal, BooleanSupplier confirmTerminal, Term initialToken) {
        operators.push(OperatorEntry.TERMINAL); // adds a guard for handleEnd
        State oldState = state;
        try {
            state = State.ARG_OR_PREFIX;
            for (Term token = initialToken; token != PrologEOF.EOF; token = tokenizer.nextToken()) {
                if (token == Interned.OPEN_BRACKET) {
                    if (state == State.OPERATOR) {
                        handleStructure();
                    } else {
                        handleBracketTerm();
                    }
                } else if (token == Interned.OPEN_SQUARE_BRACKET) {
                    handleList();
                } else if (token == Interned.OPEN_BRACES) {
                    handleBraces();
                } else if (token == terminal && confirmTerminal.getAsBoolean()) {
                    return handleEnd(terminal);
                } else {
                    handleTerm(token);
                }
            }
            // EOF
            if (state == State.ARG_OR_PREFIX && operators.peek() == OperatorEntry.TERMINAL) {
                // if no terms read, this is an acceptable place to receive an end-of-file
                operators.pop();
                return PrologEOF.EOF;
            }
            throw PrologSyntaxError.eofError(environment,
                    "EOF reached before term was completed, missing '" + terminal + "' ?");
        } finally {
            state = oldState;
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
        if (!(atom instanceof PrologAtom)) {
            throw PrologSyntaxError.functorError(environment, "Functor expected before '('");
        }
        Term result = read(Interned.CLOSE_BRACKET, () -> true, tokenizer.nextToken());
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
        Term result = read(Interned.CLOSE_BRACKET, () -> true, tokenizer.nextToken());
        handleTerm(rewriteBracketedCommas(result));
    }

    /**
     * Handle content of '{' ... '}'
     */
    private void handleBraces() {
        Term next = tokenizer.nextToken();
        if (next == Interned.CLOSE_BRACES) {
            handleTerm(Interned.EMPTY_BRACES_ATOM);
        } else {
            Term nestedTerm = read(Interned.CLOSE_BRACES, () -> true, next);
            handleTerm(nestedTerm);
            throw new UnsupportedOperationException("NYI"); // not sure what to do here, yet
        }
    }

    /**
     * Handle content of '[' ... ']'
     */

    private void handleList() {
        Term next = tokenizer.nextToken();
        if (next == Interned.CLOSE_SQUARE_BRACKET) {
            handleTerm(PrologEmptyList.EMPTY_LIST);
        } else {
            Term nestedTerm = read(Interned.CLOSE_SQUARE_BRACKET, () -> true, next);
            handleTerm(convertList(nestedTerm));
        }
    }

    /**
     * Handle self-identifying term, constant, operator, etc
     *
     * @param term Self identifying term
     */
    private void handleTerm(Term term) {
        OperatorEntry op = OperatorEntry.ARGUMENT;
        if (term instanceof Atomic) {
            if (state == State.OPERATOR) {
                op = environment.getInfixPostfixOperator((Atomic) term);
                // TODO: if operator is quoted, maybe allow as compound term (see SWI Prolog)
                // See isNext() method of tokenizer to help with this
            } else {
                op = environment.getPrefixOperator((Atomic) term);
                if (op == OperatorEntry.ARGUMENT) {
                    op = environment.getInfixPostfixOperator((Atomic)term);
                    if (op != OperatorEntry.ARGUMENT) {
                        OperatorEntry lastOp = operators.peek();
                        if (lastOp.getCode().isPrefix() &&
                                (op == Operators.COMMA || lastOp.getPrecedence() < op.getPrecedence())) {
                            // re-interpret last operator as non-operator
                            operators.pop();
                            state = State.OPERATOR;
                        } else {
                            op = OperatorEntry.ARGUMENT;
                        }
                    }
                }
            }
        }

        if (op == OperatorEntry.ARGUMENT) {
            if (state == State.OPERATOR) {
                // Syntax error, an operator was expected
                throw PrologSyntaxError.expectedOperatorError(environment, "Unexpected term " + term + " where operator is expected");
            } else {
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
        }

        //
        // At this point, we are looking at an operator
        //
        if (state == State.ARG_OR_PREFIX) {
            // Prefix operators just get pushed
            operators.push(op);
            stack.push(term); // save the operator for unrolling
            // keep processing args and prefix operators
            return;
        }

        //
        // Only binary and postfix operators remain, reduction can now occur
        //
        while (operators.peek() != OperatorEntry.TERMINAL) {
            OperatorEntry lastOp = operators.pop();
            int compare = lastOp.compareTo(op);
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
        if (op.getCode().isPostfix()) {
            // postfix operators are reduced immediately
            Atomic atom = (Atomic) term;
            Term theArg = stack.pop();
            stack.push(new CompoundTermImpl(atom, theArg));
            state = State.OPERATOR; // still looking for operator
        } else {
            // infix operators have to wait until more parsing is done
            operators.push(op);
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
            if (atom == Interned.MINUS_ATOM && theArg.isNumber()) {
                // negate immediately
                theArg = ((PrologNumber) theArg).negate();
                stack.push(theArg);
            } else if (atom == Interned.PLUS_ATOM && theArg.isNumber()) {
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
    private void reinterpretLastOperator(PrologAtom terminal) {
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
            if (terminal == Interned.DOT) {
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
    private Term handleEnd(PrologAtom terminal) {
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
