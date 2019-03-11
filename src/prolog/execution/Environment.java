// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

import prolog.bootstrap.Builtins;
import prolog.bootstrap.DefaultIoBinding;
import prolog.bootstrap.Interned;
import prolog.bootstrap.Operators;
import prolog.constants.Atomic;
import prolog.constants.PrologAtom;
import prolog.exceptions.PrologError;
import prolog.exceptions.PrologPermissionError;
import prolog.expressions.Term;
import prolog.functions.StackFunction;
import prolog.io.IoBinding;
import prolog.predicates.BuiltInPredicate;
import prolog.predicates.ClauseSearchPredicate;
import prolog.predicates.DemandLoadPredicate;
import prolog.predicates.MissingPredicate;
import prolog.predicates.PredicateDefinition;
import prolog.predicates.Predication;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Runtime environment of Prolog. Note that in the current version, Environments are not thread safe. That is, only
 * one thread may use an Environment.
 */
public class Environment {

    // table of atoms for this instance
    private final HashMap<String, PrologAtom> atomTable = new HashMap<>();
    // table of predicates for this instance
    private final HashMap<Predication, PredicateDefinition> dictionary = new HashMap<>();
    // table of functions functions
    private final HashMap<Predication, StackFunction> functions = new HashMap<>();
    // tables of operators for this instance
    private final HashMap<Atomic, OperatorEntry> infixPostfixOperatorTable = new HashMap<>();
    private final HashMap<Atomic, OperatorEntry> prefixOperatorTable = new HashMap<>();
    // io
    private final HashMap<Atomic, IoBinding> streams = new HashMap<>();
    // stacks
    private final LinkedList<InstructionPointer> callStack = new LinkedList<>();
    private final LinkedList<Backtrack> backtrackStack = new LinkedList<>();
    private final LinkedList<Term> dataStack = new LinkedList<>();
    private IoBinding readBinding = DefaultIoBinding.USER_INPUT;
    private IoBinding writeBinding = DefaultIoBinding.USER_OUTPUT;
    private Path cwd = Paths.get(".").normalize().toAbsolutePath();
    private CatchPoint catchPoint = CatchPoint.TERMINAL;
    // state
    private ExecutionState executionState = ExecutionState.FORWARD;
    // terminals
    private final InstructionPointer terminalIP = () -> executionState = ExecutionState.SUCCESS;
    private final Backtrack backtrackTerminal = new Backtrack() {
        @Override
        public String toString() {
            return "terminal";
        }

        @Override
        public void undo() {
            executionState = ExecutionState.FAILED;
        }
    };
    private InstructionPointer ip = terminalIP;
    // Variable ID allocator
    private long nextVariableId = 1;
    // local localContext used for variable binding
    private LocalContext localContext = new LocalContext(this, Predication.UNDEFINED);

    /**
     * Construct a new environment.
     */
    public Environment() {
        // Bootstap
        dictionary.putAll(Builtins.getPredicates());
        functions.putAll(Builtins.getFunctions());
        infixPostfixOperatorTable.putAll(Operators.getInfixPostfix());
        prefixOperatorTable.putAll(Operators.getPrefix());
        streams.putAll(DefaultIoBinding.getSystem());
        // Add atoms last to ensure that all interned atoms are added
        atomTable.putAll(Interned.getInterned());
    }

    /**
     * New local context for this environment.
     *
     * @return new local context
     */
    public LocalContext newLocalContext(Predication predication) {
        return new LocalContext(this, predication);
    }

    /**
     * New local context of same predication as previous local context.
     *
     * @return new local context
     */
    public LocalContext newLocalContext() {
        return newLocalContext(getLocalContext().getPredication());
    }

    /**
     * @return local context
     */
    public LocalContext getLocalContext() {
        return localContext;
    }

    /**
     * Change local context
     *
     * @param context New context
     */
    public void setLocalContext(LocalContext context) {
        localContext = context;
    }

    /**
     * @return true if progressing forward
     */
    public boolean isForward() {
        return executionState == ExecutionState.FORWARD;
    }

    /**
     * Restore IP from stack (return).
     */
    public void restoreIP() {
        ip = callStack.pop();
    }

    /**
     * Push IP and Change IP (Call).
     *
     * @param ip New IP
     */
    public void callIP(InstructionPointer ip) {
        callStack.push(this.ip);
        this.ip = ip;
    }

    /**
     * @return current IP
     */
    public InstructionPointer getIP() {
        return this.ip;
    }

    /**
     * Push term to data stack. Currently only used for number evaluation.
     *
     * @param term Term to push
     */
    public void push(Term term) {
        dataStack.push(term);
    }

    /**
     * Retrieve data from data stack. Currently only used for number evaluation.
     *
     * @return term retrieved from stack
     */
    public Term pop() {
        return dataStack.pop();
    }

    /**
     * Depth of IP stack.
     *
     * @return depth
     */
    public int getCallStackDepth() {
        return callStack.size();
    }

    /**
     * Depth of Data stack.
     *
     * @return depth
     */
    public int getDataStackDepth() {
        return dataStack.size();
    }

    /**
     * Depth of Backtrack stack. This is also used as a cut marker.
     *
     * @return depth
     */
    public int getBacktrackDepth() {
        return backtrackStack.size();
    }

    /**
     * Allocates a unique number for each variable.
     *
     * @return Variable ID
     */
    public long nextVariableId() {
        return nextVariableId++;
    }

    /**
     * On (e.g.) exception handling, backtrack stack is reduced to a known point, undoing any operations along the way.
     *
     * @param depth Desired depth (from prior call to {@link #getBacktrackDepth()}).
     */
    public void trimBacktrackStackToDepth(int depth) {
        while (backtrackStack.size() > depth) {
            backtrackStack.pop().undo();
        }
    }

    /**
     * On (e.g.) exception handling, data stack is reduced to a known point.
     */
    public void trimDataStack(int depth) {
        while (dataStack.size() > depth) {
            dataStack.pop();
        }
    }

    /**
     * Capture return stack at decision point.
     *
     * @return snapshot of stack for future call to {@link #restoreStack(InstructionPointer[])}.
     */
    public InstructionPointer[] constructStack() {
        // TODO: potential point of optimization.
        InstructionPointer[] stack = new InstructionPointer[callStack.size() + 1];
        stack[0] = ip.copy();
        int i = 1;
        // copy stack
        // Copy is required here for 2nd and subsequent backtrack visits to behave
        // correctly. That is, the copied state is independent of the current stack
        // state.
        // Where possible, copy is a NO-OP.
        ListIterator<InstructionPointer> iter = callStack.listIterator(0);
        while (i < stack.length) {
            stack[i++] = iter.next().copy();
        }
        return stack;
    }

    /**
     * Restore stack at decision point.
     *
     * @param stack Captured snapshot from prior call to {@link #constructStack()}.
     */
    public void restoreStack(InstructionPointer[] stack) {
        // Current implementations restores stack completely.
        callStack.clear();
        // Copy is required here for 3rd and subsequent backtrack visits to behave
        // correctly. That is, the restored state is independent of the saved state.
        // Where possible, copy is a NO-OP.
        for (int i = stack.length - 1; i > 0; i--) {
            callStack.push(stack[i].copy());
        }
        ip = stack[0].copy();
    }

    /**
     * Perform a cut. The local context identifies the limit of the cut. The backtrack stack is iterated with an
     * attempt to erase as many backtrack entries as possible (particularly decision points).
     */
    public void cutDecisionPoints() {
        int depth = getLocalContext().getCutDepth(); // depth of scope prior to cut
        if (depth != LocalContext.DETERMINISTIC) {
            // the local context will be considered deterministic, set this now so
            // conditional cuts can operate
            getLocalContext().setCutDepth(LocalContext.DETERMINISTIC);
            //
            // Determine how far back we can rewind backtracking
            //
            int delta = backtrackStack.size() - depth;
            //
            // Attempt to reduce the stack
            //
            ListIterator<Backtrack> iter = backtrackStack.listIterator();
            while (delta-- > 0) {
                iter.next().cut(iter);
            }
        }
    }

    /**
     * Save a backtracking entry point. This will be executed on
     * backtracking.
     *
     * @param backtrack Backtracking state/callback
     */
    public void pushBacktrack(Backtrack backtrack) {
        backtrackStack.push(backtrack);
    }

    /**
     * Begin backtracking
     */
    public void backtrack() {
        executionState = ExecutionState.BACKTRACK;
    }

    /**
     * Resume progressing forward
     */
    public void forward() {
        executionState = ExecutionState.FORWARD;
    }

    /**
     * Enter into a throwing/exception state
     *
     * @param term Term representing cause
     */
    public void throwing(Term term) {
        for (; ; ) {
            if (catchPoint.tryCatch(term)) {
                // handled
                return;
            }
            // catchPoint will have been updated
        }
    }

    /**
     * @return inner most catch point.
     */
    public CatchPoint getCatchPoint() {
        return catchPoint;
    }

    /**
     * Set/restore catch point (e.g. as part of a decision point)
     *
     * @param catchPoint New catch point
     */
    public void setCatchPoint(CatchPoint catchPoint) {
        this.catchPoint = catchPoint;
    }

    /**
     * Perform a complete reset of state.
     */
    public void reset() {
        forward();
        callStack.clear();
        backtrackStack.clear();
        dataStack.clear();
        ip = terminalIP;
        backtrackStack.push(backtrackTerminal);
        catchPoint = CatchPoint.TERMINAL;
    }

    /**
     * Main execution transition between FORWARD and BACKTRACK until a terminal state is hit.
     *
     * @return true if success
     */
    public ExecutionState run() {
        // Tight loop handling forward and backtracking at the simplest level
        for (; ; ) {
            try {
                while (executionState == ExecutionState.FORWARD) {
                    ip.next();
                }
                while (executionState == ExecutionState.BACKTRACK) {
                    backtrackStack.poll().backtrack();
                }
                if (executionState.isTerminal()) {
                    return executionState;
                }
            } catch (RuntimeException e) {
                // convert Java exceptions into Prolog exceptions
                throwing(PrologError.convert(this, e));
            }
        }
    }

    /**
     * Create or retrieve an atom of specified name for this environment. Only one atom exists per name per environment.
     *
     * @param name Name of atom
     * @return Atom
     */
    public PrologAtom getAtom(String name) {
        return atomTable.computeIfAbsent(name, PrologAtom::internalNew);
    }

    /**
     * Adds a new built-in predicate specific to this environment, overwriting any existing predicate. Scoped only
     * to this environment.
     *
     * @param functor   Functor of predicate
     * @param arity     Arity of predicate
     * @param predicate Actual predicate
     */
    public void setBuiltinPredicate(Atomic functor, int arity, BuiltInPredicate predicate) {
        Predication key = new Predication(functor, arity);
        dictionary.put(key, predicate);
    }

    /**
     * Retrieve predicate for the specified clause name and arity.
     *
     * @param predication Functor/Arity
     * @return predicate definition
     */
    public PredicateDefinition lookupPredicate(Predication predication) {
        PredicateDefinition entry = dictionary.get(predication);
        if (entry == null) {
            return MissingPredicate.MISSING_PREDICATE;
        } else {
            return entry;
        }
    }

    /**
     * Remove predicate for the specified clause name and arity.
     *
     * @param predication functor/arity
     * @return old definition
     */
    public PredicateDefinition abolishPredicate(Predication predication) {
        PredicateDefinition entry = dictionary.remove(predication);
        if (entry == null) {
            return MissingPredicate.MISSING_PREDICATE;
        } else {
            return entry;
        }
    }

    /**
     * Retrieve a function. Used for evaluation.
     *
     * @param predication functor/arity
     * @return StackFunction or null
     */
    public StackFunction lookupFunction(Predication predication) {
        return functions.get(predication);
    }

    /**
     * Create predicate for the specified clause name and arity as needed.
     *
     * @param predication Predication
     * @return predicate definition
     */
    public PredicateDefinition autoCreateDictionaryEntry(Predication predication) {
        return dictionary.computeIfAbsent(predication, ClauseSearchPredicate::new);
    }

    /**
     * Create predicate for the specified clause name and arity. Note that it will also replace a
     * {@link DemandLoadPredicate}.
     *
     * @param predication Functor/arity
     * @return predicate definition
     */
    public ClauseSearchPredicate createDictionaryEntry(Predication predication) {
        PredicateDefinition entry = autoCreateDictionaryEntry(predication);
        if (entry instanceof ClauseSearchPredicate) {
            return (ClauseSearchPredicate) entry;
        } else if (entry instanceof DemandLoadPredicate) {
            // force replacement of demand-load predicates
            ClauseSearchPredicate replace = new ClauseSearchPredicate(predication);
            dictionary.put(predication, replace);
            return replace;
        } else {
            throw PrologPermissionError.error(this, "modify", "static_procedure", predication.term(),
                    "Cannot override built-in procedure");
        }
    }

    /**
     * Retrieve operator entry for given atom, prefix
     *
     * @param atom Operator atom
     * @return operator entry
     */
    public OperatorEntry getPrefixOperator(Atomic atom) {
        OperatorEntry entry = prefixOperatorTable.get(atom);
        if (entry == null) {
            return OperatorEntry.ARGUMENT;
        } else {
            return entry;
        }
    }

    /**
     * Retrieve operator entry for given atom, infix or postfix
     *
     * @param atom Operator atom
     * @return operator entry
     */
    public OperatorEntry getInfixPostfixOperator(Atomic atom) {
        OperatorEntry entry = infixPostfixOperatorTable.get(atom);
        if (entry == null) {
            return OperatorEntry.ARGUMENT;
        } else {
            return entry;
        }
    }

    /**
     * Current context path for open.
     *
     * @return Context path (Current working directory)
     */
    public Path getCWD() {
        return cwd;
    }

    /**
     * Change context path for open
     *
     * @param newCwd New path
     */
    public void setCWD(Path newCwd) {
        this.cwd = this.cwd.resolve(newCwd).normalize();
    }

    /**
     * Change reader. The binding is specified, not the read/write stream itself, so that
     * the stream identifier/alias can be retrieved.
     *
     * @param reader New reader
     * @return old reader
     */
    public IoBinding setReader(IoBinding reader) {
        IoBinding oldReader = readBinding;
        this.readBinding = reader;
        return oldReader;
    }

    /**
     * Change writer. The binding is specified, not the read/write stream itself, so that
     * the stream identifier/alias can be retrieved.
     *
     * @param writer New writer
     * @return old writer
     */
    public IoBinding setWriter(IoBinding writer) {
        IoBinding oldWriter = writeBinding;
        this.writeBinding = writer;
        return oldWriter;
    }

    /**
     * Get current reader
     *
     * @return reader
     */
    public IoBinding getReader() {
        return readBinding;
    }

    /**
     * Get current writer
     *
     * @return writer
     */
    public IoBinding getWriter() {
        return writeBinding;
    }

    /**
     * Retrieve existing stream binding by name
     *
     * @param name Name of stream
     * @return stream
     */
    public IoBinding lookupStream(Atomic name) {
        return streams.get(name);
    }

    /**
     * Add stream binding by name
     *
     * @param name       Stream name
     * @param newBinding New stream
     * @return old stream of same name
     */
    public IoBinding addStream(Atomic name, IoBinding newBinding) {
        return streams.put(name, newBinding);
    }

    /**
     * Update/add operator precedence.
     *
     * @param precedence Precedence to set
     * @param code       operator code
     * @param atom       Operator atom
     */
    public void makeOperator(int precedence, OperatorEntry.Code code, PrologAtom atom) {
        OperatorEntry entry;
        if (code.isPrefix()) {
            entry = prefixOperatorTable.computeIfAbsent(atom, OperatorEntry::new);
        } else {
            entry = infixPostfixOperatorTable.computeIfAbsent(atom, OperatorEntry::new);
        }
        entry.setCode(code);
        entry.setPrecedence(precedence);
    }

}
