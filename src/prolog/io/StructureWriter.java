// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.io;

import prolog.bootstrap.Interned;
import prolog.bootstrap.Operators;
import prolog.constants.Atomic;
import prolog.constants.PrologAtom;
import prolog.constants.PrologCharacter;
import prolog.constants.PrologCodePoints;
import prolog.constants.PrologEmptyList;
import prolog.execution.Environment;
import prolog.execution.OperatorEntry;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.flags.WriteOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Helper class to format a structure based on operator precedence and preferred syntax sugar. For example,
 * how lists are structured.
 */
public class StructureWriter extends TermWriter<Term> {

    private static final int COMMA_UNSAFE = 0x0001; // set if commas need wrapping

    private static boolean test(int flags, int bits) {
        return (flags & bits) != 0;
    }

    private final OpCompare terminal = new OpCompare(OperatorEntry.TERMINAL);
    private final OpCompare comma = new OpCompare(Operators.COMMA);

    public StructureWriter(WriteContext context, Term term) {
        super(context, term);
    }

    @Override
    public void write() throws IOException {
        write(terminal, 0, term);
    }

    private void write(OpCompare opComp, int flags, Term term) throws IOException {
        if (term instanceof CompoundTerm) {
            writeCompound(opComp, flags, (CompoundTerm) term);
        } else {
            term.write(context);
        }
    }

    private void writeCompound(OpCompare opComp, int flags, CompoundTerm term) throws IOException {
        Atomic functor = term.functor();
        int arity = term.arity();
        if (functor == Interned.COMMA_FUNCTOR && test(flags, COMMA_UNSAFE)) {
            context.beginSafe();
            context.write("(");
            writeCompound(terminal, 0, term);
            context.beginSafe();
            context.write(")");
            return;
        }
        if (functor == Interned.LIST_FUNCTOR && arity == 2) {
            writeSquareList(term);
            return;
        }
        if (arity == 2) {
            OperatorEntry op = context.environment().getInfixPostfixOperator(functor);
            if (op != OperatorEntry.ARGUMENT && op.getCode().isBinary()) {
                writeBinaryOperator(opComp, flags, op, functor, term.get(0), term.get(1));
                return;
            }
        }
        if (arity == 1) {
            OperatorEntry op = context.environment().getPrefixOperator(functor);
            if (op != OperatorEntry.ARGUMENT && op.getCode().isPrefix()) {
                writePrefixOperator(flags, op, functor, term.get(0));
                return;
            }
            op = context.environment().getInfixPostfixOperator(functor);
            if (op != OperatorEntry.ARGUMENT && op.getCode().isPostfix()) {
                writePostfixOperator(flags, op, functor, term.get(0));
                return;
            }
        }
        writeStructure(term);
    }

    private void writeBinaryOperator(OpCompare opComp, int flags, OperatorEntry op, Atomic func, Term left, Term right) throws IOException {
        // P * Q * R * S
        if (opComp.binaryBrackets(op)) {
            context.beginSafe();
            context.write("(");
            writeBinaryOperator(terminal, 0, op, func, left, right);
            context.beginSafe();
            context.write(")");
            return;
        }
        write(new LeftCompare(op), flags, left);
        func.write(context);
        write(new RightCompare(op), flags, right);
    }

    private void writePrefixOperator(int flags, OperatorEntry op, Atomic func, Term term) throws IOException {
        func.write(context);
        write(new OpCompare(op), flags, term);
    }

    private void writePostfixOperator(int flags, OperatorEntry op, Atomic func, Term term) throws IOException {
        write(new OpCompare(op), flags, term);
        func.write(context);
    }

    private void writeStructure(CompoundTerm term) throws IOException {
        // TODO conflict if structure is a prefix operator, not handled
        write(terminal, 0, term.functor());
        int arity = term.arity();
        if (term.arity() == 0) {
            return;
        }
        context.beginSafe();
        context.write("(");
        write(comma, COMMA_UNSAFE, term.get(0));
        for (int i = 1; i < arity; i++) {
            context.beginSafe();
            context.write(", ");
            write(comma, COMMA_UNSAFE, term.get(i));
        }
        context.beginSafe();
        context.write(")");
    }

    private void writeSquareList(CompoundTerm term) throws IOException {
        // First build up the list, analyzing what we expect to create
        if (term instanceof PrologCodePoints) {
            term.write(context);
            return;
        }
        ArrayList<Term> list = new ArrayList<>();
        Term tail = term;
        boolean stringish = true;
        StringBuilder builder = new StringBuilder();
        while (CompoundTerm.termIsA(tail, Interned.LIST_FUNCTOR, 2)) {
            CompoundTerm comp = (CompoundTerm) tail;
            Term item = comp.get(0);
            tail = comp.get(1);
            if (stringish) {
                // assume it might be a string until we prove it's not a string
                if (item instanceof PrologCharacter) {
                    builder.append((char) ((PrologCharacter) item).get());
                } else {
                    if (item instanceof PrologAtom) {
                        String name = ((PrologAtom) item).name();
                        if (name.length() == 1) {
                            builder.append(name.charAt(0));
                        } else {
                            stringish = false;
                        }
                    } else {
                        stringish = false;
                    }
                }
            }
            list.add(item);
        }
        if (stringish && tail == PrologEmptyList.EMPTY_LIST) {
            // This can be written as a string
            PrologCodePoints cp = new PrologCodePoints(builder.toString());
            cp.write(context);
            return;
        }
        context.beginSafe();
        context.write("[");
        Iterator<Term> iter = list.iterator();
        write(comma, COMMA_UNSAFE, iter.next());
        while (iter.hasNext()) {
            context.beginSafe();
            context.write(", ");
            write(comma, COMMA_UNSAFE, iter.next());
        }
        if (tail != PrologEmptyList.EMPTY_LIST) {
            context.beginSafe();
            context.write("| ");
            // | is lower precedence than comma, but this is arguably more readable
            write(comma, COMMA_UNSAFE, tail);
        }
        context.beginSafe();
        context.write("]");
    }

    private class OpCompare {
        final OperatorEntry parent;

        OpCompare(OperatorEntry entry) {
            this.parent = entry;
        }

        boolean binaryBrackets(OperatorEntry op) {
            return parent.getPrecedence() <= op.getPrecedence();
        }

    }

    private class LeftCompare extends OpCompare {

        LeftCompare(OperatorEntry entry) {
            super(entry);
        }

        @Override
        boolean binaryBrackets(OperatorEntry leftOp) {
            if (leftOp.getPrecedence() < parent.getPrecedence()) {
                // e.g. a*b+c parent=+, op=*
                return false;
            }
            if (leftOp.getPrecedence() > parent.getPrecedence()) {
                // e.g. (a+b)*c parent=*, op=+
                return true;
            }
            if (leftOp.getCode() == OperatorEntry.Code.YFX && parent.getCode() == OperatorEntry.Code.YFX) {
                // left to right natural precedence
                return false;
            }
            // All other cases, use parenthesis
            return true;
        }
    }

    private class RightCompare extends OpCompare {

        RightCompare(OperatorEntry entry) {
            super(entry);
        }

        @Override
        boolean binaryBrackets(OperatorEntry rightOp) {
            if (parent.getPrecedence() > rightOp.getPrecedence()) {
                // e.g. a+b*c parent=+, op=*
                return false;
            }
            if (parent.getPrecedence() < rightOp.getPrecedence()) {
                // e.g. a*(b+c) parent=*, op=+
                return true;
            }
            if (parent.getCode() == OperatorEntry.Code.XFY && rightOp.getCode() == OperatorEntry.Code.XFY) {
                // right to left natural precedence
                return false;
            }
            // All other cases, use parenthesis
            return true;
        }
    }

    /**
     * Convert a term to a formatted/structured string.
     *
     * @param environment Execution environment
     * @param term        Term to convert
     * @return String form
     */
    public static String toString(Environment environment, Term term) {
        try (StringOutputStream output = new StringOutputStream()) {
            WriteContext context = new WriteContext(
                    environment,
                    new WriteOptions(environment, null),
                    output);
            StructureWriter structWriter = new StructureWriter(context, term);
            structWriter.write();
            return output.toString();
        } catch (IOException e) {
            throw new InternalError(e.getMessage(), e);
        }
    }
}
