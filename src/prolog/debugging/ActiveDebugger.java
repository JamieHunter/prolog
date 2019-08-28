// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

import prolog.bootstrap.Builtins;
import prolog.bootstrap.DefaultIoBinding;
import prolog.constants.PrologAtomInterned;
import prolog.execution.Backtrack;
import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.InstructionPointer;
import prolog.expressions.CompoundTerm;
import prolog.flags.WriteOptions;
import prolog.instructions.ExecRunClause;
import prolog.io.Prompt;
import prolog.predicates.Predication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * Class that implements an active debugger hook.
 */
public class ActiveDebugger implements DebuggerHook {
    /*package*/ final Environment environment;
    /*package*/ final SpyPoints spyPoints;
    private final WeakHashMap<Instruction, InstructionContext> instructionMap = new WeakHashMap<>();
    private final WeakHashMap<DecisionPoint, InstructionContext> decisionMap = new WeakHashMap<>();
    private final WeakHashMap<InstructionPointer, InstructionContext> callMap = new WeakHashMap<>();
    private static final HashMap<String, Function<String, StepMode>> dispatch = new HashMap<>();
    private static final ArrayList<String> helpTable = new ArrayList<>();
    private StepMode mode = StepMode.LEAP;
    private ExecutionPort port = null;
    private int id = 0;
    private InstructionContext prologThis = InstructionContext.NULL;

    public ActiveDebugger(Environment environment) {
        this.environment = environment;
        this.spyPoints = environment.spyPoints();
        addCommand("c", this::creep, "creep");
        addCommand("l", this::leap, "leap");
        addCommand("s", this::undefined, "skip", "<i> skip i");
        addCommand("o", this::undefined, "out", "<n> out n");
        addCommand("q", this::undefined, "q-skip", "<i> q-skip i");
        addCommand("r", this::undefined, "retry", "<i> retry i");
        addCommand("f", this::failCurrent, "fail");
        addCommand("i", this::ignoreCurrent, "ignore");
        addCommand("d", this::undefined, "display");
        addCommand("p", this::undefined, "print");
        addCommand("w", this::undefined, "write");
        addCommand("g", this::undefined, "ancestors", "<n> ancestors n");
        addCommand("t", this::undefined, "backtrace", "<n> backtrace n");
        addCommand("n", this::undefined, "nodebug");
        addCommand("=", this::undefined, "debugging");
        addCommand("+", this::activateSpy, "spy this", "<i> spy conditionally");
        addCommand("-", this::removeSpy, "nospy this");
        addCommand(".", this::undefined, "find this");
        addCommand("a", this::undefined, "abort");
        addCommand("b", this::undefined, "break");
        addCommand("@", this::undefined, "command");
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
     * Called when resetting environment
     */
    @Override
    public void reset() {
    }

    /**
     * Called when a new decisionpoint is pushed, this may be referenced during backtrack to identify redo port.
     * @param decisionPoint Decision point
     */
    @Override
    public void decisionPoint(DecisionPoint decisionPoint) {
        if (prologThis != InstructionContext.NULL) {
            decisionMap.put(decisionPoint, prologThis);
        }
    }

    /**
     * Progressing forward to next instruction
     *
     * @param ip Instruction Pointer
     */
    @Override
    public void forward(InstructionPointer ip) {
        // handle return port at time first instruction of the ip is executed
        // TODO: if copy was called, and this is the copy, it might not be recognized.
        // I'm not sure we need to handle that / or desirable to handle that.
        InstructionContext deferred = callMap.remove(ip);
        if (deferred != null) {
            invoke(deferred, ExecutionPort.RETURN, null);
        }
        InstructionContext newContext = contextFromInstruction(instructionFromIp(ip));
        invoke(newContext, ExecutionPort.CALL, ()->ip.next());
    }

    /**
     * Push IP to stack. this inherently means that the Exit/Fail port
     * us unknown until return
     * @param ip Instruction pointer
     */
    @Override
    public void pushIP(InstructionPointer ip) {
        if (prologThis != InstructionContext.NULL) {
            callMap.put(ip, prologThis);
            prologThis = InstructionContext.NULL;
            port = ExecutionPort.DEFERRED;
        }
    }

    /**
     * Wrap around a direct instruction invoke
     *
     * @param unused Used by {@link NoDebugger}, not used here.
     * @param inst Instruction
     */
    @Override
    public void invoke(Environment unused, Instruction inst) {
        //
        // E.g. CALL executing instruction directly
        invoke(contextFromInstruction(inst), ExecutionPort.CALL, ()->inst.invoke(environment));
    }

    /**
     * Backtracking
     *
     * @param bt Backtrack marker
     */
    @Override
    public void backtrack(Backtrack bt) {
        if (!(bt instanceof DecisionPoint)) {
            bt.backtrack();
            return;
        }
        invoke(contextFromDecisionPoint((DecisionPoint)bt), ExecutionPort.REDO, ()->bt.backtrack());
    }

    private void invoke(InstructionContext newContext, ExecutionPort newPort, Runnable action) {
        if (newContext == InstructionContext.NULL) {
            if (action != null) {
                action.run();
            }
            return;
        }
        // Allow nesting
        InstructionContext oldThis = prologThis;
        ExecutionPort oldPort = port;
        try {
            prologThis = newContext;
            port = newPort;
            int flags = mode.flags(this, prologThis);
            if (port != ExecutionPort.RETURN) {
                // CALL or REDO port
                if (flags == 0) {
                    action.run();
                    return;
                }
                if ((flags & port.flag()) != 0) {
                    enterPort();
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
            if ((flags & port.flag()) != 0) {
                enterPort();
            }
        } finally {
            port = oldPort;
            prologThis = oldThis;
        }
    }

    /**
     * Given an IP, map to instruction
     *
     * @param ip Instruction pointer
     * @return Instruction
     */
    private Instruction instructionFromIp(InstructionPointer ip) {
        if (ip instanceof InstructionReporter) {
            return ((InstructionReporter) ip).peek();
        } else {
            return null;
        }
    }

    /**
     * Given an Instruction, map to a Context unique for this unique instruction.
     *
     * @param inst Instruction
     * @return Context
     */
    private InstructionContext contextFromInstruction(Instruction inst) {
        if (inst == null) {
            return InstructionContext.NULL;
        }
        return instructionMap.computeIfAbsent(inst, this::computeContext);
    }

    /**
     * Given a decision point reference, map to a Context unique for this unique point.
     *
     * @param dp DecisionPoint reference
     * @return Context
     */
    private InstructionContext contextFromDecisionPoint(DecisionPoint dp) {
        return decisionMap.getOrDefault(dp, InstructionContext.NULL);
    }

    /**
     * When an Instruction does not have a context, build one.
     *
     * @param inst Instruction
     * @return Context for instruction.
     */
    private InstructionContext computeContext(Instruction inst) {
        InstructionContext context = InstructionContext.NULL;
        if (inst instanceof InstructionReflection) {
            CompoundTerm reflected = ((InstructionReflection) inst).reflect();
            if (reflected == null) {
                return context;
            }
            context = computeContextFromCompoundTerm(inst, reflected);
        } else if (inst != null) {
            InstructionLookup lookup = Builtins.getLookup(inst);
            if (lookup != null) {
                context = computeContextFromLookup(lookup);
            }
        }
        return context;
    }

    /**
     * Given a Compound Term, and no context, build one.
     *
     * @param instruction Instruction
     * @param source      Source compound term
     * @return Context for source
     */
    private InstructionContext computeContextFromCompoundTerm(Instruction instruction, CompoundTerm source) {
        PrologAtomInterned functor = PrologAtomInterned.from(environment, source.functor());
        Predication.Interned predication = new Predication.Interned(functor, source.arity());
        return new InstructionContext(predication, instruction, source, ++id);
    }

    /**
     * Given an InstructionLookup (immutable), build a context.
     *
     * @param lookup InstructionLookup from builtins table
     * @return Context
     */
    private InstructionContext computeContextFromLookup(InstructionLookup lookup) {
        CompoundTerm source = lookup.reflect();
        SpySpec spySpec = SpySpec.from((PrologAtomInterned) source.functor(), source.arity());
        if (Builtins.isNoTrace(spySpec)) {
            return InstructionContext.NULL; // trace blacklist
        }
        return new InstructionContext(lookup, ++id);
    }

    /**
     * Enter debug loop for given port
     */
    private void enterPort() {
        do {
            traceIsSpy();
            traceId();
            traceDepth();
            traceText(port.display() + ": ");
        } while (command());
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

    private void traceIsSpy() {
        if (prologThis.spyFlags(spyPoints) != 0) {
            traceText("+ ");
        } else {
            traceText("  ");
        }
    }

    private void traceId() {
        traceText(String.format("%4d ", prologThis.getId()));
    }

    private void traceDepth() {
        traceText(String.format("%4d ", environment.getCallStackDepth()));
    }

    private void traceGoal(InstructionContext context) {
        CompoundTerm goal = context.reflect().resolve(environment.getLocalContext());
        if (context == InstructionContext.NULL) {
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
        traceGoal(prologThis);
        traceText(" ? ");
        traceFlush();
        String cmd = readLine();
        cmd = cmd.trim();
        if (cmd.length() == 0) {
            mode = creep("");
            return false;
        }
        String[] parts = cmd.split("\\s+", 2);
        String arg = "";
        if (parts.length > 1) {
            arg = parts[1];
        }
        Function<String, StepMode> func = dispatch.get(parts[0]);
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

    private StepMode creep(String arg) {
        return StepMode.CREEP;
    }

    private StepMode leap(String arg) {
        return StepMode.LEAP;
    }

    private StepMode undefined(String arg) {
        traceText("NYI\n");
        return null;
    }

    private StepMode activateSpy(String arg) {
        SpySpec spySpec = prologThis.spySpec();
        spyPoints.addSpy(spySpec);
        return null;
    }

    private StepMode removeSpy(String arg) {
        SpySpec spySpec = prologThis.spySpec();
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
}
