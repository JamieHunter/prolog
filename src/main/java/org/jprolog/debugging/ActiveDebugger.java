// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.debugging;

import org.jprolog.bootstrap.DefaultIoBinding;
import org.jprolog.bootstrap.Interned;
import org.jprolog.cli.Run;
import org.jprolog.constants.PrologAtomInterned;
import org.jprolog.constants.PrologEOF;
import org.jprolog.exceptions.PrologAborted;
import org.jprolog.execution.Backtrack;
import org.jprolog.execution.CompileContext;
import org.jprolog.execution.DecisionPoint;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.execution.InstructionPointer;
import org.jprolog.execution.Query;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.CompoundTermImpl;
import org.jprolog.expressions.Term;
import org.jprolog.flags.ReadOptions;
import org.jprolog.flags.WriteOptions;
import org.jprolog.io.Prompt;
import org.jprolog.library.Debug;
import org.jprolog.parser.StringParser;
import org.jprolog.predicates.BuiltInPredicate;
import org.jprolog.predicates.ClauseEntry;
import org.jprolog.predicates.ClauseSearchPredicate;
import org.jprolog.predicates.OnDemand;
import org.jprolog.predicates.PredicateDefinition;
import org.jprolog.predicates.Predication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Class that implements an active debugger hook.
 */
public class ActiveDebugger implements DebuggerHook {
    /*package*/ final Environment environment;
    /*package*/ final SpyPoints spyPoints;
    private final WeakHashMap<DebugInstruction, InstructionContext> instructionMap = new WeakHashMap<>();
    private final WeakHashMap<Object, Scoped> exitMap = new WeakHashMap<>();
    private static final HashMap<String, Function<String, StepMode>> dispatch = new HashMap<>();
    private static final ArrayList<String> helpTable = new ArrayList<>();
    private StepMode mode = StepMode.LEAP;
    private ExecutionPort port = null;
    private int id = 0;
    private long sequence = 0;
    private long skipSeqId = 0;
    private Scoped prologThis = Scoped.NULL;

    public ActiveDebugger(Environment environment) {
        this.environment = environment;
        this.spyPoints = environment.spyPoints();
        addCommand("c", this::creep, "creep");
        addCommand("l", this::leap, "leap");
        addCommand("s", this::skip, "skip", "<i> skip i");
        addCommand("o", this::out, "out", "<n> out n");
        addCommand("q", this::qskip, "q-skip", "<i> q-skip i");
        addCommand("r", this::undefined, "retry", "<i> retry i");
        addCommand("f", this::failCurrent, "fail");
        addCommand("i", this::ignoreCurrent, "ignore");
        addCommand("d", this::write, "display");
        addCommand("p", this::print, "print");
        addCommand("w", this::display, "write");
        addCommand("g", this::ancestors, "ancestors", "<n> ancestors n");
        addCommand("t", this::backtrace, "backtrace", "<n> backtrace n");
        addCommand("n", this::nodebug, "nodebug");
        addCommand("=", this::debuggingStatus, "debugging");
        addCommand("+", this::activateSpy, "spy this", "<i> spy conditionally");
        addCommand("-", this::removeSpy, "nospy this");
        addCommand(".", this::undefined, "find this");
        addCommand("a", this::abort, "abort");
        addCommand("b", this::doBreak, "break");
        addCommand("@", this::doCommand, "command");
        addCommand("u", this::undefined, "unify");
        addCommand("e", this::undefined, "raise exception");
        addCommand("<", this::undefined, "reset printdepth", "<n> set printdepth");
        addCommand("^", this::undefined, "reset subterm", "<n> set subterm");
        addCommand("?", this::commandHelp, "help");
        addCommand("h", this::commandHelp, "help");
    }

    private void addCommand(String cmd, Function<String, StepMode> func, String... help) {
        dispatch.put(cmd, func);
        for (String text : help) {
            helpTable.add(cmd + " " + text);
        }
    }

    /**
     * Flag to stop at next instruction
     */
    @Override
    public void trace() {
        mode = StepMode.CREEP;
    }

    /**
     * A 'DebugInstruction' will call invoke, to instrument debug ports either side of the
     * instruction.
     *
     * @param unused      Used by {@link NoDebugger}, not used here.
     * @param context     Provides information previously inserted by debug-compile.
     * @param instruction Original instruction to call
     */
    @Override
    public void invoke(Environment unused, DebugInstruction context, Instruction instruction) {
        //
        // E.g. CALL executing instruction directly
        invoke(getCallScope(context), ExecutionPort.CALL, () -> instruction.invoke(environment));
    }

    /**
     * A debuggable decision point, after a restore and just before the point of redo.
     *
     * @param context       Provides information previously collected when decision point was inserted.
     * @param decisionPoint Original decision point.
     */
    @Override
    public void redo(DebugDecisionPoint context, DecisionPoint decisionPoint) {
        Scoped scoped = context.getScope();
        scoped.incrementIteration();
        invoke(scoped, ExecutionPort.REDO, decisionPoint::redo);
    }

    /**
     * Called when a new DecisionPoint is pushed, this may be referenced during backtrack to identify redo port.
     * The decision point indicates that the instruction may backtrack, but doesn't mean it will, nor does it mean
     * that the instruction is deferred.
     *
     * @param decisionPoint Decision point
     * @return decoratedDecisionPoint
     */
    @Override
    public DecisionPoint acceptDecisionPoint(DecisionPoint decisionPoint) {
        if (prologThis.instructionContext() != InstructionContext.NULL) {
            if (!(decisionPoint instanceof DebugDecisionPoint)) {
                decisionPoint = new DebugDecisionPoint(environment, decisionPoint, prologThis);
            }
        }
        return decisionPoint;
    }

    /**
     * Called when new IP has been pushed. This implicitly means that the scope has been deferred until the IP is
     * resumed.
     *
     * @param ip New instruction pointer
     * @return decorated instruction pointer
     */
    @Override
    public void acceptIP(InstructionPointer ip) {
        if (prologThis.instructionContext() != InstructionContext.NULL && port != ExecutionPort.DEFERRED) {
            // consider ip to be either (a) the execution block for the current instruction, or
            // (b) special return context. In either case, leaving ip (see restore) amounts to an exit
            exitMap.put(ip.ref(), prologThis);
            port = ExecutionPort.DEFERRED;
        }
    }

    /**
     * Called when IP leaves execution scope, that is, we return back to the caller that had previously
     * pushed IP
     *
     * @param ip IP being exited
     */
    @Override
    public void leaveIP(InstructionPointer ip) {
        Scoped scope = exitMap.get(ip.ref());
        if (scope != null) {
            invoke(scope, ExecutionPort.RETURN, null);
        }
    }

    /**
     * Create a debugging aware compile context.
     *
     * @param shared Shared aspect of Environment(s).
     * @return Compile context
     */
    @Override
    public CompileContext newCompileContext(Environment.Shared shared) {
        return new DebuggingCompileContext(shared);
    }

    protected boolean isSkipEnd(Scoped scope) {
        // reached an instruction that was entered earlier or same sequence than the specified watermark
        // used during call/exit ports
        return scope.getSeqId() <= skipSeqId;
    }

    /**
     * Common logic for handling entry/exit ports.
     *
     * @param scope     Scope, includes iteration, parent context, and instruction
     * @param enterPort Port on entry or {@link ExecutionPort#RETURN} if handling exit port only.
     * @param action    Action to perform on entry port, or null if handling exit port only.
     */
    private void invoke(Scoped scope, ExecutionPort enterPort, Runnable action) {
        if (scope.instructionContext() == InstructionContext.NULL) {
            if (action != null) {
                action.run();
            }
            return;
        }
        // Allow nesting
        Scoped oldThis = prologThis;
        ExecutionPort oldPort = port;
        try {
            prologThis = scope;
            port = enterPort;
            if (port != ExecutionPort.RETURN) {
                int enterFlags = mode.flags(this, scope);
                // CALL or REDO port
                if (enterFlags == 0) {
                    action.run();
                    return;
                }
                if ((enterFlags & port.flag()) != 0) {
                    portDebugLoop();
                    if (mode.ignore()) {
                        return;
                    }
                }
                action.run();
                // port may have been changed to DEFERRED at this point
                if (port == ExecutionPort.DEFERRED) {
                    return; // a call occurred, we'll resume this later
                }
            }
            // Any of (1) return from invoke with no stack push
            // (2) return from redoing, with no stack push
            // (3) trapped detected stack-based return
            // assume EXIT or FAIL
            if (environment.isForward()) {
                port = ExecutionPort.EXIT;
            } else {
                port = ExecutionPort.FAIL;
            }
            int exitFlags = mode.flags(this, scope);
            if ((exitFlags & port.flag()) != 0) {
                portDebugLoop();
            }
        } finally {
            port = oldPort;
            prologThis = oldThis;
        }
    }

    /**
     * Given an Instruction at a call point, Create a unique scope object. This scope object tracks the local
     * context on entry to the instruction (context of parent), and tracks the iterations of redo's.
     *
     * @param inst Instruction
     * @return Context
     */
    private Scoped getCallScope(DebugInstruction inst) {
        if (inst == null) {
            return Scoped.NULL;
        }
        InstructionContext context = instructionMap.computeIfAbsent(inst, this::computeInstructionContext);
        if (context == InstructionContext.NULL) {
            return Scoped.NULL;
        }
        return new Scoped(context, environment.getLocalContext(), inst.isTraceable(), ++sequence);
    }

    /**
     * When an Instruction does not have a context, build one.
     *
     * @param instruction Instruction
     * @return Context for instruction.
     */
    private InstructionContext computeInstructionContext(DebugInstruction instruction) {
        CompoundTerm source = instruction.getSource();
        PrologAtomInterned functor = PrologAtomInterned.from(environment, source.functor());
        Predication.Interned predication = new Predication.Interned(functor, source.arity());
        return new InstructionContext(predication, instruction, ++id);
    }

    /**
     * Enter debug loop for given port
     */
    private void portDebugLoop() {
        do {
            traceContext(prologThis, getCallStack(-1).size(), port.display() + ": ");
        } while (command());
    }

    /**
     * Retrieve call stack using scoping contextual information. Ignore extra nested stacks.
     *
     * @param limit Max list size. -1 indicates any. 0 indicates test IP only (special case).
     *              >0 indicates a max list of size limit.
     * @return Filtered call stack
     */
    private List<Scoped> getCallStack(int limit) {
        if (limit < 0) {
            limit = Integer.MAX_VALUE;
        }
        ArrayList<Scoped> stack = new ArrayList<>();
        Scoped top = exitMap.get(environment.getIP().ref());
        if (top != null && top != prologThis) {
            stack.add(top);
            limit--;
        }
        ListIterator<InstructionPointer> it = environment.getCallStack().listIterator(environment.getCallStackDepth());
        while (it.hasPrevious() && limit > 0) {
            InstructionPointer ip = it.previous();
            Scoped scoped = exitMap.get(ip);
            if (scoped != null) {
                stack.add(scoped);
            }
        }
        return stack;
    }

    /**
     * @return Decision stack
     */
    private List<Scoped> getDecisionStack() {
        ArrayList<Scoped> stack = new ArrayList<>();
        ListIterator<Backtrack> it = environment.getBacktrackStack().listIterator(environment.getBacktrackDepth());
        Backtrack bt = it.previous();
        if (bt instanceof DebugDecisionPoint) {
            Scoped scoped = ((DebugDecisionPoint) bt).getScope();
            stack.add(scoped);
        }
        return stack;
    }

    private void traceContext(Scoped scoped, int depth, String portText) {
        traceIsSpy(scoped);
        traceId(scoped);
        traceDepth(depth);
        if (portText != null) {
            traceText(portText);
        }
        traceGoal(scoped);
    }

    private void traceText(String text) {
        DefaultIoBinding.USER_ERROR.write(environment, null, text);
    }

    private void traceFlush() {
        try {
            DefaultIoBinding.USER_ERROR.flush();
        } catch (IOException e) {
            // ignored
        }
    }

    private void traceNL() {
        traceText("\n");
    }

    private void traceIsSpy(Scoped scoped) {
        if (scoped.instructionContext().spyFlags(spyPoints) != 0) {
            traceText("+ ");
        } else {
            traceText("  ");
        }
    }

    private void traceId(Scoped scoped) {
        traceText(String.format("%4d ", scoped.instructionContext().getId()));
    }

    private void traceDepth(int depth) {
        traceText(String.format("%4d ", depth));
    }

    private void traceGoal(Scoped scoped) {
        CompoundTerm goal = scoped.instructionContext().getSource().resolve(scoped.localContext());
        if (scoped.instructionContext() == InstructionContext.NULL) {
            return;
        }
        WriteOptions options = new WriteOptions(environment, null);
        options.quoted = true;
        DefaultIoBinding.USER_ERROR.write(environment, null, goal, options);
    }

    /**
     * Read line of input, with no prompt.
     *
     * @return Line of input
     */
    private String readLine() {
        DefaultIoBinding.USER_INPUT.setPrompt(environment, null, Prompt.NONE);
        return DefaultIoBinding.USER_INPUT.readLine(environment, null, null);
    }

    /**
     * Prompt for command.
     *
     * @return true to reprompt, false to resume
     */
    private boolean command() {
        try {
            traceText(" ? ");
            traceFlush();
            String line = readLine();
            if (line == null) {
                abort();
            }
            line = line.trim();
            if (line.length() == 0) {
                mode = creep("");
                return false;
            }
            String cmd = line.substring(0, 1);
            String arg = line.substring(1).trim();
            Function<String, StepMode> func = dispatch.get(cmd);
            if (func != null) {
                StepMode newMode = func.apply(arg);
                if (newMode != null) {
                    mode = newMode;
                    return false;
                } else {
                    return true;
                }
            } else {
                shortHelp();
                return true;
            }
        } catch (PrologAborted pa) {
            throw pa;
        } catch (RuntimeException re) {
            String text = re.getMessage();
            traceText("Error: " + text);
            return true;
        }
    }

    /**
     * Helper for integer arguments
     *
     * @param arg Argument to parse
     * @return Optional of Integer
     */
    private Optional<Integer> parseInteger(String arg) {
        if (arg == null || arg.length() == 0) {
            return Optional.empty();
        } else {
            return Optional.of(Integer.parseInt(arg));
        }
    }

    private void shortHelp() {
        StringBuilder builder = new StringBuilder();
        for (String t : helpTable) {
            builder.append(t.charAt(0));
            builder.append(' ');
        }
        traceText(builder.toString());
    }

    private StepMode commandHelp(String arg) {
        StringBuilder builder = new StringBuilder();
        int pos = 0;
        for (String t : helpTable) {
            switch (pos++) {
                case 0:
                    builder.append(t);
                    break;
                case 1:
                    builder.append('\t');
                    builder.append(t);
                    break;
                case 2:
                    builder.append('\n');
                    builder.append(t);
                    pos = 1;
                    break;
            }
        }
        builder.append('\n');
        traceText(builder.toString());
        return null;
    }

    private StepMode undefined(String arg) {
        traceText("NYI\n");
        return null;
    }

    private StepMode creep(String arg) {
        return StepMode.CREEP;
    }

    private StepMode leap(String arg) {
        return StepMode.LEAP;
    }

    private StepMode skip(String arg) {
        return skip(arg, StepMode.SKIP);
    }

    private StepMode qskip(String arg) {
        return skip(arg, StepMode.QSKIP);
    }

    private StepMode skip(String arg, StepMode mode) {
        Optional<Integer> iarg = parseInteger(arg);
        skipSeqId = prologThis.getSeqId();
        if (iarg.isPresent()) {
            boolean hitCall = false;
            ListIterator<Scoped> callIt = getCallStack(-1).listIterator();
            while (callIt.hasPrevious()) {
                Scoped scoped = callIt.previous();
                if (hitCall || scoped.instructionContext().getId() == iarg.get()) {
                    hitCall = true;
                    skipSeqId = scoped.getSeqId();
                }
            }
            if (hitCall) {
                return mode;
            }
        }
        if (port == ExecutionPort.EXIT || port == ExecutionPort.FAIL || iarg.isPresent()) {
            traceText("Unable to skip\n");
            return null;
        } else {
            return mode;
        }
    }

    private StepMode out(String arg) {
        int iarg = parseInteger(arg).orElse(0);
        List<Scoped> calls;
        if (iarg <= 0) {
            calls = Collections.emptyList();
        } else {
            calls = getCallStack(iarg);
        }
        if (calls.size() < 1) {
            skipSeqId = prologThis.getSeqId() - 1;
        } else {
            skipSeqId = calls.get(calls.size() - 1).getSeqId() - 1;
        }
        return StepMode.SKIP;
    }

    private StepMode activateSpy(String arg) {
        SpySpec spySpec = prologThis.instructionContext().spySpec();
        spyPoints.addSpy(spySpec);
        return null;
    }

    private StepMode removeSpy(String arg) {
        SpySpec spySpec = prologThis.instructionContext().spySpec();
        spyPoints.removeSpy(spySpec);
        return null;
    }

    private StepMode failCurrent(String arg) {
        if (!port.canIgnore()) {
            traceText("Cannot fail at this point\n");
        }
        environment.backtrack();
        return StepMode.IGNORE_AND_CREEP;
    }

    private StepMode ignoreCurrent(String arg) {
        if (!port.canIgnore()) {
            traceText("Cannot ignore at this point\n");
        }
        environment.forward();
        return StepMode.IGNORE_AND_CREEP;
    }

    private StepMode ancestors(String arg) {
        Optional<Integer> iarg = parseInteger(arg);
        List<Scoped> stack = getCallStack(-1);
        int max = iarg.orElse(stack.size());
        if (max < 1 || max > stack.size()) {
            max = stack.size();
        }
        int pos = stack.size() - max;
        for (Scoped scoped : stack) {
            traceContext(scoped, pos, null);
            traceNL();
            if (++pos > max) {
                break;
            }
        }
        return null;
    }

    private StepMode backtrace(String arg) {
        Optional<Integer> iarg = parseInteger(arg);
        List<Scoped> stack = getDecisionStack();
        int max = iarg.orElse(stack.size());
        if (max < 1 || max > stack.size()) {
            max = stack.size();
        }
        int pos = stack.size() - max;
        for (Scoped scoped : stack) {
            traceContext(scoped, pos, null);
            traceNL();
            if (++pos > max) {
                break;
            }
        }
        return null;
    }

    protected StepMode write(String arg) {
        WriteOptions options = new WriteOptions(environment, null);
        options.quoted = true;
        options.fullstop = true;
        options.nl = true;
        writePredicateCommon(t -> DefaultIoBinding.USER_ERROR.write(environment, null, t, options));
        return null;
    }

    private StepMode display(String arg) {
        return write(arg);
    }

    private StepMode print(String arg) {
        return write(arg);
    }

    private void writePredicateCommon(Consumer<Term> writer) {
        CompoundTerm term = prologThis.instructionContext().getSource();
        Predication pred = term.toPredication();
        PredicateDefinition defn = environment.lookupPredicate(pred);
        if (defn instanceof BuiltInPredicate) {
            traceText("Builtin Predicate.\n");
            return;
        } else if (defn instanceof OnDemand) {
            traceText("Predicate not yet defined (demand-load).\n");
        } else if (!(defn instanceof ClauseSearchPredicate)) {
            traceText("Predicate not defined.\n");
        }
        ClauseEntry[] clauses = ((ClauseSearchPredicate) defn).getClauses();
        ClauseEntry entry = clauses[prologThis.getIteration()];
        CompoundTerm clause = new CompoundTermImpl(Interned.CLAUSE_FUNCTOR, entry.getHead(), entry.getBody());
        writer.accept(clause);
    }

    private StepMode debuggingStatus(String arg) {
        Debug.debugging(environment, DefaultIoBinding.USER_ERROR);
        return null;
    }

    private StepMode abort(String arg) {
        abort();
        return null; // unreached
    }

    private void abort() {
        throw PrologAborted.abort(environment);
    }

    private StepMode doBreak(String arg) {
        traceText("Break: type 'abort.' to return\n");
        new Run(new Environment(environment)).run();
        return null;
    }

    private StepMode nodebug(String arg) {
        environment.enableDebugger(false);
        return StepMode.NODEBUG;
    }

    /**
     * Execute command given as an argument
     *
     * @param arg Command argument
     * @return
     */
    private StepMode doCommand(String arg) {
        ReadOptions options = new ReadOptions(environment, null);
        options.fullStop = ReadOptions.FullStop.ATOM_optional;
        Term term = StringParser.parse(environment, arg, options);
        if (term == PrologEOF.EOF) {
            traceText("Use as: @command");
            return null;
        }
        Query query = new Query(new Environment(environment));
        query.compile(term);
        query.run();
        return null;
    }
}
