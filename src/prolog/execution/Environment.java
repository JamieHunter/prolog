// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

import prolog.bootstrap.Builtins;
import prolog.bootstrap.DefaultIoBinding;
import prolog.bootstrap.Interned;
import prolog.bootstrap.Operators;
import prolog.constants.Atomic;
import prolog.constants.PrologAtomInterned;
import prolog.constants.PrologAtomLike;
import prolog.constants.PrologInteger;
import prolog.exceptions.PrologError;
import prolog.exceptions.PrologPermissionError;
import prolog.expressions.Term;
import prolog.flags.PrologFlags;
import prolog.functions.StackFunction;
import prolog.io.LogicalStream;
import prolog.parser.CharConverter;
import prolog.predicates.BuiltInPredicate;
import prolog.predicates.ClauseSearchPredicate;
import prolog.predicates.DemandLoadPredicate;
import prolog.predicates.LoadGroup;
import prolog.predicates.MissingPredicate;
import prolog.predicates.PredicateDefinition;
import prolog.predicates.Predication;
import prolog.predicates.VarArgDefinition;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Runtime environment of Prolog. Note that in the current version, Environments are not thread safe. That is, only
 * one thread may use an Environment.
 */
public class Environment {

    // character translation table
    private final CharConverter charConverter = new CharConverter();
    // table of atoms for this instance
    private final HashMap<String, PrologAtomInterned> atomTable = new HashMap<>();
    // table of predicates for this instance
    private final HashMap<Predication.Interned, PredicateDefinition> dictionary = new HashMap<>();
    // table of variable argument predicates for this instance
    private final HashMap<PrologAtomInterned, VarArgDefinition> varArgDictionary = new HashMap<>();
    // table of functions functions
    private final HashMap<Predication.Interned, StackFunction> functions = new HashMap<>();
    // tables of operators for this instance
    private final TreeMap<Atomic, OperatorEntry> infixPostfixOperatorTable = new TreeMap<>();
    private final TreeMap<Atomic, OperatorEntry> prefixOperatorTable = new TreeMap<>();
    // io, ID mappings
    private final HashMap<PrologInteger, LogicalStream> streamById = new HashMap<>();
    private final HashMap<PrologAtomInterned, LogicalStream> streamByAlias = new HashMap<>();
    // load group mappings
    private final HashMap<String, LoadGroup> loadGroups = new HashMap<>();
    // stacks
    private final LinkedList<InstructionPointer> callStack = new LinkedList<>();
    private final LinkedList<Backtrack> backtrackStack = new LinkedList<>();
    private final LinkedList<Term> dataStack = new LinkedList<>();
    private LogicalStream inputStream = DefaultIoBinding.USER_INPUT;
    private LogicalStream outputStream = DefaultIoBinding.USER_OUTPUT;
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
    private long nextVariableId = 10;
    // local localContext used for variable binding
    private LocalContext localContext = new LocalContext(this, Predication.UNDEFINED, CutPoint.TERMINAL);
    // global flags
    private final PrologFlags flags = new PrologFlags(this);
    // how to handle a cut
    private CutPoint cutPoint = localContext;
    // current load group
    private LoadGroup loadGroup;

    /**
     * Construct a new environment.
     */
    public Environment() {
        // Bootstap
        dictionary.putAll(Builtins.getPredicates());
        varArgDictionary.putAll(Builtins.getVarArgPredicates());
        functions.putAll(Builtins.getFunctions());
        infixPostfixOperatorTable.putAll(Operators.getInfixPostfix());
        prefixOperatorTable.putAll(Operators.getPrefix());
        streamById.putAll(DefaultIoBinding.getById());
        streamByAlias.putAll(DefaultIoBinding.getByAlias());
        // Add atoms last to ensure that all interned atoms are added
        atomTable.putAll(Interned.getInterned());
        // By default, all definitions are associated with this special load group
        changeLoadGroup(new LoadGroup.Interactive());
    }

    /**
     * New local context for this environment.
     *
     * @return new local context
     */
    public LocalContext newLocalContext(Predication predication) {
        return new LocalContext(this, predication, cutPoint);
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
     * Perform a cut. Behavior is delegated.
     */
    public void cutDecisionPoints() {
        cutPoint.cut();
    }

    /**
     * Retrieve current cutPoint
     *
     * @return cutpoint
     */
    public CutPoint getCutPoint() {
        return cutPoint;
    }

    /**
     * Change cutpoint
     *
     * @param newCutPoint new cutpoint handler
     */
    public void setCutPoint(CutPoint newCutPoint) {
        cutPoint = newCutPoint;
    }

    /**
     * Primitive helper of cut
     *
     * @param targetDepth new target depth of backtrack stack
     */
    public int cutBacktrackStack(int targetDepth) {
        //
        // Determine how far back we can rewind backtracking
        //
        int delta = backtrackStack.size() - targetDepth;
        //
        // Attempt to reduce the stack
        //
        ListIterator<Backtrack> iter = backtrackStack.listIterator();
        while (delta-- > 0) {
            iter.next().cut(iter);
        }
        return backtrackStack.size();
    }

    /**
     * Save a backtracking entry point. This will be executed on
     * backtracking. No updates are made to any CutPoints
     *
     * @param backtrack Backtracking state/callback
     */
    public void pushBacktrack(Backtrack backtrack) {
        backtrackStack.push(backtrack);
    }

    /**
     * Add a decision point.
     *
     * @param decisionPoint New decision point
     */
    public void pushDecisionPoint(DecisionPoint decisionPoint) {
        cutPoint.markDecisionPoint(backtrackStack.size());
        pushBacktrack(decisionPoint);
    }

    /**
     * Conditionally add a backtrack
     */
    public void pushBacktrackIfNotDeterministic(Backtrack backtrack) {
        if (!cutPoint.isDeterministic()) {
            pushBacktrack(backtrack);
        }
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
     * @return Interned Atom
     */
    public PrologAtomInterned internAtom(String name) {
        return atomTable.computeIfAbsent(name, PrologAtomInterned::internalNew);
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
        Predication.Interned interned = new Predication.Interned(PrologAtomInterned.from(this, functor), arity);
        dictionary.put(interned, predicate);
    }

    /**
     * Retrieve predicate for the specified clause name and arity.
     *
     * @param predication Functor/Arity
     * @return predicate definition
     */
    public PredicateDefinition lookupPredicate(Predication predication) {
        Predication.Interned interned = predication.intern(this);
        PredicateDefinition entry = dictionary.get(interned);
        if (entry == null) {
            entry = lookupVarArgPredicate(interned);
        }
        if (entry == null) {
            entry = MissingPredicate.MISSING_PREDICATE;
        }
        return entry;
    }

    /**
     * Look up a variable-argument predicate if one exists
     *
     * @param predication Predication to look up
     * @return variable-argument predicate, or null
     */
    private PredicateDefinition lookupVarArgPredicate(Predication predication) {
        Predication.Interned interned = predication.intern(this);
        VarArgDefinition varArg = varArgDictionary.get(interned.functor());
        if (varArg == null) {
            return null;
        }
        return varArg.lookup(interned);
    }

    /**
     * Remove predicate for the specified clause name and arity.
     *
     * @param predication functor/arity
     * @return old definition
     */
    public PredicateDefinition abolishPredicate(Predication predication) {
        Predication.Interned interned = predication.intern(this);
        PredicateDefinition entry = dictionary.remove(interned);
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
        return functions.get(predication.intern(this));
    }

    /**
     * Create predicate for the specified clause name and arity as needed.
     *
     * @param predication Predication
     * @return predicate definition
     */
    public PredicateDefinition autoCreateDictionaryEntry(Predication predication) {
        Predication.Interned interned = predication.intern(this);
        return dictionary.computeIfAbsent(interned, this::autoPredicate);
    }

    /**
     * Used when there is a predicate miss. Either use a variable-argument predicate, or create an empty predicate
     * entry. Note that a "miss" will result in the predicate being auto-populated into the predicate table.
     *
     * @param predication Predication to "auto create"
     * @return variable-argument predicate, or new predicate
     */
    private PredicateDefinition autoPredicate(Predication predication) {
        PredicateDefinition defn = lookupVarArgPredicate(predication);
        if (defn == null) {
            defn = new ClauseSearchPredicate();
        }
        return defn;
    }

    /**
     * Create predicate for the specified clause name and arity. Note that it will also replace a
     * {@link DemandLoadPredicate}.
     *
     * @param predication Functor/arity
     * @return predicate definition
     */
    public ClauseSearchPredicate createDictionaryEntry(Predication predication) {
        Predication.Interned interned = predication.intern(this);
        PredicateDefinition entry = autoCreateDictionaryEntry(interned);
        if (entry instanceof ClauseSearchPredicate) {
            return (ClauseSearchPredicate) entry;
        } else if (entry instanceof DemandLoadPredicate) {
            // force replacement of demand-load predicates
            ClauseSearchPredicate replace = new ClauseSearchPredicate(interned);
            dictionary.put(interned, replace);
            return replace;
        } else {
            throw PrologPermissionError.error(this, "modify", "static_procedure", interned.term(),
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
     * Retrieve all prefix operators for iteration
     *
     * @return all prefix operators
     */
    public Map<Atomic, OperatorEntry> getPrefixOperators() {
        return Collections.unmodifiableMap(prefixOperatorTable);
    }

    /**
     * Retrieve all infix/postfix operators for iteration
     *
     * @return all infix/postfix operators
     */
    public Map<Atomic, OperatorEntry> getInfixPostfixOperators() {
        return Collections.unmodifiableMap(infixPostfixOperatorTable);
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
     * Change input stream.
     *
     * @param logicalStream New stream
     * @return old stream
     */
    public LogicalStream setInputStream(LogicalStream logicalStream) {
        LogicalStream oldStream = inputStream;
        this.inputStream = logicalStream;
        return oldStream;
    }

    /**
     * Change output stream.
     *
     * @param logicalStream New stream
     * @return old stream
     */
    public LogicalStream setOutputStream(LogicalStream logicalStream) {
        LogicalStream oldStream = outputStream;
        this.outputStream = logicalStream;
        return oldStream;
    }

    /**
     * Get current input stream
     *
     * @return input stream
     */
    public LogicalStream getInputStream() {
        return inputStream;
    }

    /**
     * Get current output stream
     *
     * @return output stream
     */
    public LogicalStream getOutputStream() {
        return outputStream;
    }

    /**
     * Retrieve existing stream by id or alias
     *
     * @param streamIdent Stream identifier
     * @return stream or null if not found
     */
    public LogicalStream lookupStream(Atomic streamIdent) {
        if (streamIdent.isInteger()) {
            return streamById.get(streamIdent);
        } else if (streamIdent.isAtom()) {
            return streamByAlias.get(PrologAtomInterned.from(this, streamIdent));
        } else {
            return null;
        }
    }

    /**
     * Retrieve a list of all open streams
     *
     * @return all open streams
     */
    public Collection<LogicalStream> getOpenStreams() {
        return streamById.values();
    }

    /**
     * Add stream by unique id
     *
     * @param id     Stream id
     * @param stream Stream
     */
    public void addStream(PrologInteger id, LogicalStream stream) {
        if (stream != null) {
            streamById.put(id, stream);
        }
    }

    /**
     * Remove stream by unique id
     *
     * @param id     Stream id
     * @param stream Stream - must match to delete
     */
    public void removeStream(PrologInteger id, LogicalStream stream) {
        if (stream != null) {
            streamById.remove(id, stream);
        }
    }

    /**
     * Add stream by unique id
     *
     * @param alias  Stream alias (if null, becomes a no-op)
     * @param stream Stream
     * @return previous stream with this alias, or null if none
     */
    public LogicalStream addStreamAlias(PrologAtomInterned alias, LogicalStream stream) {
        if (alias == null || stream == null) {
            return null;
        }
        LogicalStream prior;
        prior = streamByAlias.put(alias, stream);
        if (prior != stream) {
            if (prior != null) {
                prior.removeAlias(alias);
            }
            if (stream != null) {
                stream.addAlias(alias);
            }
        }
        return prior;
    }

    /**
     * Remove stream alias
     *
     * @param alias  Stream alias (if null, becomes a no-op)
     * @param stream Stream - must match to delete
     */
    public void removeStreamAlias(PrologAtomInterned alias, LogicalStream stream) {
        if (alias == null || stream == null) {
            return;
        }
        streamByAlias.remove(alias, stream);
        stream.removeAlias(alias);
    }

    /**
     * Update/add operator precedence.
     *
     * @param precedence Precedence to set
     * @param code       operator code
     * @param atom       Operator atom
     */
    public void makeOperator(int precedence, OperatorEntry.Code code, PrologAtomInterned atom) {
        OperatorEntry entry;
        if (code.isPrefix()) {
            entry = prefixOperatorTable.computeIfAbsent(atom, OperatorEntry::new);
        } else {
            entry = infixPostfixOperatorTable.computeIfAbsent(atom, OperatorEntry::new);
        }
        entry.setCode(code);
        entry.setPrecedence(precedence);
    }

    /**
     * Delete operator precedence.
     *
     * @param code operator code (determines if operator is prefix or not).
     * @param atom Operator atom
     */
    public void removeOperator(OperatorEntry.Code code, PrologAtomInterned atom) {
        OperatorEntry entry;
        if (code.isPrefix()) {
            entry = prefixOperatorTable.remove(atom);
        } else {
            entry = infixPostfixOperatorTable.remove(atom);
        }
    }

    /**
     * Retrieve active load group
     *
     * @return load group
     */
    public LoadGroup getLoadGroup() {
        return loadGroup;
    }

    /**
     * Get load group by id
     *
     * @param id Load group identifier
     * @return load group, or empty
     */
    public LoadGroup getLoadGroup(String id) {
        return loadGroups.get(id);
    }

    /**
     * Change load group. Group is inserted into table, replacing
     * any previous group of that name
     *
     * @param group New load group
     */
    public void changeLoadGroup(LoadGroup group) {
        loadGroups.put(group.getId(), group);
        loadGroup = group;
    }

    /**
     * Retrieve the prolog flags structure
     *
     * @return Flags
     */
    public PrologFlags getFlags() {
        return flags;
    }

    /**
     * Retrieve the character conversion table
     *
     * @return conversion table
     */
    public CharConverter getCharConverter() {
        return charConverter;
    }
}
