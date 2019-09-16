// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.execution;

import org.jprolog.bootstrap.Builtins;
import org.jprolog.bootstrap.DefaultIoBinding;
import org.jprolog.bootstrap.Interned;
import org.jprolog.bootstrap.Operators;
import org.jprolog.callstack.ActiveExecutionPoint;
import org.jprolog.callstack.ExecutionPoint;
import org.jprolog.callstack.ExecutionSpliterator;
import org.jprolog.callstack.ExecutionTerminal;
import org.jprolog.callstack.ResumableExecutionPoint;
import org.jprolog.constants.Atomic;
import org.jprolog.constants.PrologAtomInterned;
import org.jprolog.constants.PrologInteger;
import org.jprolog.cuts.CutPoint;
import org.jprolog.cuts.CutThroughDecision;
import org.jprolog.debugging.ActiveDebugger;
import org.jprolog.debugging.DebuggerHook;
import org.jprolog.debugging.NoDebugger;
import org.jprolog.debugging.SpyPoints;
import org.jprolog.exceptions.PrologError;
import org.jprolog.exceptions.PrologHalt;
import org.jprolog.exceptions.PrologPermissionError;
import org.jprolog.expressions.Term;
import org.jprolog.flags.CloseOptions;
import org.jprolog.flags.PrologFlags;
import org.jprolog.functions.StackFunction;
import org.jprolog.io.LogicalStream;
import org.jprolog.parser.CharConverter;
import org.jprolog.predicates.BuiltInPredicate;
import org.jprolog.predicates.ClauseSearchPredicate;
import org.jprolog.predicates.DemandLoadPredicate;
import org.jprolog.predicates.LoadGroup;
import org.jprolog.predicates.MissingPredicate;
import org.jprolog.predicates.PredicateDefinition;
import org.jprolog.predicates.Predication;
import org.jprolog.predicates.VarArgDefinition;
import org.jprolog.utility.TrackableList;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Runtime environment of Prolog. Note that in the current version, Environments are not thread safe. That is, only
 * one thread may use an Environment.
 */
public class Environment {

    // These are shared by all 'break' instances of Environment
    public final static class Shared {
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
        // debugging spy points
        private SpyPoints spyPoints = new SpyPoints();
        // Variable ID allocator
        private long nextVariableId = 10;
        // global flags
        private final PrologFlags flags = new PrologFlags();

        public Shared() {
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
         * Retrieve a function. Used for evaluation.
         *
         * @param predication functor/arity
         * @return StackFunction or null
         */
        public StackFunction lookupFunction(Predication predication) {
            return functions.get(predication.intern(this));
        }

        /**
         * Enumerate all known predicates as a stream.
         *
         * @return All predicates as a stream.
         */
        public Stream<Map.Entry<Predication.Interned, PredicateDefinition>> allPredicates() {
            return dictionary.entrySet().stream();
        }

        private void abortReset(Environment environment) {
            CloseOptions options = new CloseOptions(environment, null);
            for (LogicalStream stream : streamById.values()) {
                if (stream.getCloseOnAbort()) {
                    try {
                        stream.close(environment, options);
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
    }

    // shared state between related environments
    private final Shared shared;
    // stacks
    private final LinkedList<Backtrack> backtrackStack = new LinkedList<>();
    private final LinkedList<Term> dataStack = new LinkedList<>();
    // active streams
    private LogicalStream inputStream;
    private LogicalStream outputStream;
    // default streams
    private LogicalStream defaultInputStream;
    private LogicalStream defaultOutputStream;
    // active search path
    private final TrackableList<Path> searchPath = new TrackableList<>();
    // current directory
    private Path cwd;
    // catch
    private CatchPoint catchPoint = CatchPoint.TERMINAL;
    // debugger
    private DebuggerHook debuggerHook = NoDebugger.SELF;
    private boolean debugging = false;
    // state
    private ExecutionState executionState = ExecutionState.FORWARD;
    // terminals
    private final ExecutionTerminal terminalIP = new ExecutionTerminal(() -> executionState = ExecutionState.SUCCESS);
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
    private ActiveExecutionPoint execution = terminalIP;
    // local localContext used for variable binding
    private LocalContext localContext = new LocalContext(this, Predication.UNDEFINED);
    // how to handle a cut
    private CutPoint cutPoint = CutPoint.TERMINAL;
    // current load group
    private LoadGroup loadGroup;
    // break level
    private int breakLevel;

    /**
     * Construct a new environment.
     */
    public Environment() {
        this(new Shared());
    }

    /**
     * Construct a child environment (break). Prefer this over other variants to track
     * break depth and inherit some variables.
     *
     * @param parent
     */
    public Environment(Environment parent) {
        this.shared = parent.shared;
        breakLevel = parent.breakLevel + 1;
        this.defaultInputStream = this.inputStream = parent.defaultInputStream;
        this.defaultOutputStream = this.outputStream = parent.defaultOutputStream;
        this.defaultInputStream.protect(this, LogicalStream.PROTECT_INPUT);
        this.defaultOutputStream.protect(this, LogicalStream.PROTECT_OUTPUT);
        this.cwd = parent.cwd;
        changeLoadGroup(new LoadGroup.Interactive());
    }

    /**
     * Construct a child environment when parent is technically not known (e.g. loading), but shared
     * context is known.
     *
     * @param shared Environment shared context.
     */
    public Environment(Environment.Shared shared) {
        this.shared = shared;
        breakLevel = 0; // assume top level
        this.defaultInputStream = this.inputStream = DefaultIoBinding.USER_INPUT;
        this.defaultOutputStream = this.outputStream = DefaultIoBinding.USER_OUTPUT;
        this.defaultInputStream.protect(this, LogicalStream.PROTECT_INPUT);
        this.defaultOutputStream.protect(this, LogicalStream.PROTECT_OUTPUT);
        this.cwd = Paths.get(".").normalize().toAbsolutePath();
        changeLoadGroup(new LoadGroup.Interactive());
    }

    /**
     * Abort-time behavior
     */
    public void abortReset() {
        this.inputStream = this.defaultInputStream;
        this.outputStream = this.defaultOutputStream;
        shared.abortReset(this);
    }

    /**
     * Should be called prior to releasing environment
     */
    public void release() {
        this.defaultInputStream.unprotect(this, -1);
        this.defaultOutputStream.unprotect(this, -1);
        this.inputStream = this.defaultInputStream = LogicalStream.NONE;
        this.outputStream = this.defaultOutputStream = LogicalStream.NONE;
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
     * New Compile context. Depends on debugging mode.
     *
     * @return compile context
     */
    public CompileContext newCompileContext() {
        return debuggerHook.newCompileContext(shared);
    }

    /**
     * Retrieve shared context common to all 'break's.
     *
     * @return shared environment context.
     */
    public Shared getShared() {
        return shared;
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
     * Change execution
     *
     * @param ep New Execution Point
     */
    public void setExecution(ExecutionPoint ep) {
        ActiveExecutionPoint aep = ep.activate();
        if (debugging) debuggerHook.setExecution(aep);
        this.execution = aep;
    }

    /**
     * @return current execution context
     */
    public ActiveExecutionPoint getExecution() {
        return this.execution;
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
     * @param inclusive Include current executing (as if it was pushed).
     * @return Iterable list of call stack
     */
    public Stream<ResumableExecutionPoint> getCallStack() {
        return StreamSupport.stream(new ExecutionSpliterator(execution), false);
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
     * @return Iterable list of backtrack stack
     */
    public List<Backtrack> getBacktrackStack() {
        return Collections.unmodifiableList(backtrackStack);
    }

    /**
     * Allocates a unique number for each variable.
     *
     * @return Variable ID
     */
    public long nextVariableId() {
        return shared.nextVariableId++;
    }

    /**
     * Retrieve the watermark of variables introduced for the purpose of cuts.
     *
     * @return watermark (variables below this were introduced before this point).
     */
    public long variableWatermark() {
        return shared.nextVariableId;
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
        if (debugging) decisionPoint = debuggerHook.acceptDecisionPoint(decisionPoint);
        if (!cutPoint.handlesDecisionPoint()) {
            // add a cut handler for first decision point
            cutPoint = new CutThroughDecision(this, cutPoint, backtrackStack.size());
        }
        pushBacktrack(decisionPoint);
    }

    /**
     * Conditionally add a backtrack
     */
    public void pushBacktrackIfNotDeterministic(long id, Backtrack backtrack) {
        if (!cutPoint.isDeterministic(id)) {
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
        backtrackStack.clear();
        dataStack.clear();
        execution = terminalIP;
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
                    execution.invokeNext();
                }
                while (executionState == ExecutionState.BACKTRACK) {
                    backtrackStack.poll().backtrack();
                }
                if (executionState.isTerminal()) {
                    return executionState;
                }
            } catch (PrologHalt ph) {
                // I.e. PrologHalt is uncaught.
                throw ph;
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
        return shared.internAtom(name);
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
        shared.dictionary.put(interned, predicate);
    }

    /**
     * Retrieve predicate for the specified clause name and arity.
     *
     * @param predication Functor/Arity
     * @return predicate definition
     */
    public PredicateDefinition lookupPredicate(Predication predication) {
        return shared.lookupPredicate(predication);
    }

    /**
     * Remove predicate for the specified clause name and arity.
     *
     * @param predication functor/arity
     * @return old definition
     */
    public PredicateDefinition abolishPredicate(Predication predication) {
        Predication.Interned interned = predication.intern(this);
        PredicateDefinition entry = shared.dictionary.remove(interned);
        if (entry == null) {
            return MissingPredicate.MISSING_PREDICATE;
        } else {
            return entry;
        }
    }

    /**
     * Create predicate for the specified clause name and arity as needed.
     *
     * @param predication Predication
     * @return predicate definition
     */
    public PredicateDefinition autoCreateDictionaryEntry(Predication predication) {
        return shared.autoCreateDictionaryEntry(predication);
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
            shared.dictionary.put(interned, replace);
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
        OperatorEntry entry = shared.prefixOperatorTable.get(atom);
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
        OperatorEntry entry = shared.infixPostfixOperatorTable.get(atom);
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
        return Collections.unmodifiableMap(shared.prefixOperatorTable);
    }

    /**
     * Retrieve all infix/postfix operators for iteration
     *
     * @return all infix/postfix operators
     */
    public Map<Atomic, OperatorEntry> getInfixPostfixOperators() {
        return Collections.unmodifiableMap(shared.infixPostfixOperatorTable);
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
     */
    public void setInputStream(LogicalStream logicalStream) {
        this.inputStream = logicalStream;
    }

    /**
     * Change output stream.
     *
     * @param logicalStream New stream
     * @return old stream
     */
    public void setOutputStream(LogicalStream logicalStream) {
        this.outputStream = logicalStream;
    }

    /**
     * Make current streams the default streams
     */
    public void setDefaultStreams() {
        this.outputStream.protect(this, LogicalStream.PROTECT_OUTPUT);
        this.inputStream.protect(this, LogicalStream.PROTECT_INPUT);
        if (this.defaultInputStream != this.inputStream) {
            this.defaultInputStream.unprotect(this, LogicalStream.PROTECT_INPUT);
            this.defaultInputStream = this.inputStream;
        }
        if (this.defaultOutputStream != this.outputStream) {
            this.defaultOutputStream.unprotect(this, LogicalStream.PROTECT_OUTPUT);
            this.defaultOutputStream = this.outputStream;
        }
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
     * Get default input stream
     *
     * @return input stream
     */
    public LogicalStream getDefaultInputStream() {
        return defaultInputStream;
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
     * Get default output stream
     *
     * @return output stream
     */
    public LogicalStream getDefaultOutputStream() {
        return defaultOutputStream;
    }

    /**
     * Retrieve existing stream by id or alias
     *
     * @param streamIdent Stream identifier
     * @return stream or null if not found
     */
    public LogicalStream lookupStream(Atomic streamIdent) {
        if (streamIdent.isInteger()) {
            return shared.streamById.get(streamIdent);
        } else if (streamIdent.isAtom()) {
            return shared.streamByAlias.get(PrologAtomInterned.from(this, streamIdent));
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
        return shared.streamById.values();
    }

    /**
     * Add stream by unique id
     *
     * @param id     Stream id
     * @param stream Stream
     */
    public void addStream(PrologInteger id, LogicalStream stream) {
        if (stream != null) {
            shared.streamById.put(id, stream);
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
            shared.streamById.remove(id, stream);
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
        prior = shared.streamByAlias.put(alias, stream);
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
        shared.streamByAlias.remove(alias, stream);
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
            entry = shared.prefixOperatorTable.computeIfAbsent(atom, OperatorEntry::new);
        } else {
            entry = shared.infixPostfixOperatorTable.computeIfAbsent(atom, OperatorEntry::new);
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
            entry = shared.prefixOperatorTable.remove(atom);
        } else {
            entry = shared.infixPostfixOperatorTable.remove(atom);
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
        return shared.loadGroups.get(id);
    }

    /**
     * Change load group. Group is inserted into table, replacing
     * any previous group of that name
     *
     * @param group New load group
     */
    public void changeLoadGroup(LoadGroup group) {
        shared.loadGroups.put(group.getId(), group);
        loadGroup = group;
    }

    /**
     * Retrieve the prolog flags structure
     *
     * @return Flags
     */
    public PrologFlags getFlags() {
        return shared.flags;
    }

    /**
     * Retrieve the character conversion table
     *
     * @return conversion table
     */
    public CharConverter getCharConverter() {
        return shared.charConverter;
    }

    /**
     * Reference the search-path list used for recursive loads
     *
     * @return Search path
     */
    public TrackableList<Path> getSearchPath() {
        return searchPath;
    }

    /**
     * Indicate if environment debugger is enabled
     *
     * @return True if enabled
     */
    public boolean isDebuggerEnabled() {
        return debugging;
    }

    /**
     * Enable or disable debugger
     *
     * @param enabled True if debugger is enabled
     */
    public void enableDebugger(boolean enabled) {
        if (enabled == debugging) {
            return;
        }
        if (enabled) {
            debuggerHook = new ActiveDebugger(this);
        } else {
            debuggerHook = NoDebugger.SELF;
        }
        debugging = enabled;
    }

    /**
     * @return Debugger hook
     */
    public DebuggerHook debugger() {
        return debuggerHook;
    }

    /**
     * @return Trace hook
     */
    public SpyPoints spyPoints() {
        return shared.spyPoints;
    }

    /**
     * @return Break-level, 0 = top level
     */
    public int getBreakLevel() {
        return breakLevel;
    }
}
