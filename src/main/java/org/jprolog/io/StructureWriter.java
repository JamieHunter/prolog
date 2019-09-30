// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.io;

import org.jprolog.bootstrap.Interned;
import org.jprolog.bootstrap.Operators;
import org.jprolog.constants.Atomic;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.constants.PrologCharacter;
import org.jprolog.constants.PrologChars;
import org.jprolog.constants.PrologEmptyList;
import org.jprolog.constants.PrologInteger;
import org.jprolog.execution.Environment;
import org.jprolog.execution.OperatorEntry;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.Term;
import org.jprolog.flags.WriteOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Helper class to format a structure based on operator precedence and preferred syntax sugar. For example,
 * how lists are structured.
 */
public class StructureWriter extends TermWriter<Term> {

    public static final int COMMA_UNSAFE = 0x0001; // set if commas need wrapping
    public static final int NO_OPS = 0x0002; // set if compound terms use functor-prefix notation

    private static boolean test(int flags, int bits) {
        return (flags & bits) != 0;
    }

    private final OpCompare terminal = new OpCompare(OperatorEntry.TERMINAL);
    private final OpCompare comma = new OpCompare(Operators.COMMA);

    public StructureWriter(WriteContext context) {
        super(context);
    }

    /**
     * Write the term with formatting and state controlled by context.
     *
     * @param term A term to write
     * @throws IOException on IO error
     */
    @Override
    public void write(Term term) throws IOException {
        write(0, term);
    }

    /**
     * Write the term with formatting and state controlled by context, and additional flags.
     *
     * @param flags Additional flags.
     * @param term A term to write
     * @throws IOException on IO error
     */
    public void write(int flags, Term term) throws IOException {
        write(terminal, flags, term);
    }

    /**
     * Write a term with formatting, taking operator precedence into account.
     * An atomic term is just passed through.
     *
     * @param opComp Manage operator precedence
     * @param flags Additional flags
     * @param term A term to write
     * @throws IOException on error
     */
    private void write(OpCompare opComp, int flags, Term term) throws IOException {
        if (term instanceof CompoundTerm) {
            writeCompound(opComp, flags, (CompoundTerm) term);
        } else {
            term.write(context);
        }
    }

    /**
     * Write a compound term with formatting, taking operator precedence into account. This also handles $VAR,
     * lists, and `strings`.
     *
     * @param opComp Manage operator precedence
     * @param flags Additional flags
     * @param term A compound term to write
     * @throws IOException on error
     */
    private void writeCompound(OpCompare opComp, int flags, CompoundTerm term) throws IOException {
        if (context.options().ignoreOps) {
            flags |= NO_OPS;
        }
        Atomic functor = term.functor();
        int arity = term.arity();
        if (functor.is(Interned.COMMA_FUNCTOR) && test(flags, COMMA_UNSAFE)) {
            context.beginSafe();
            context.write("(");
            writeCompound(terminal, 0, term);
            context.beginSafe();
            context.write(")");
            return;
        }
        // '$VAR'(N)
        if (term.arity() == 1 && term.functor().is(Interned.DOLLAR_VAR) && context.options().numbervars) {
            writeVar(term.get(0));
            return;
        }
        if ((flags & NO_OPS) == 0) {
            if (arity == 2) {
                if (functor.is(Interned.LIST_FUNCTOR)) {
                    writeSquareList(term);
                    return;
                }
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
        }
        writeStructure(term);
    }

    /**
     * Write a binary operator as [left] OP [right]. Precedence of left and right are taken into account.
     * @param opComp Manage operator precedence
     * @param flags Additional flags
     * @param op Operator to write
     * @param func Actual functor
     * @param left Left term to write
     * @param right Right term to write
     * @throws IOException if IO error
     */
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

    /**
     * Write a prefix operator as OP [arg].
     *
     * @param flags Additional flags
     * @param op Operator to write
     * @param func Actual functor
     * @param term Argument
     * @throws IOException if IO error
     */
    private void writePrefixOperator(int flags, OperatorEntry op, Atomic func, Term term) throws IOException {
        func.write(context);
        write(new OpCompare(op), flags, term);
    }

    /**
     * Write a postfix operator as [arg] OP.
     *
     * @param flags Additional flags
     * @param op Operator to write
     * @param func Actual functor
     * @param term Argument
     * @throws IOException if IO error
     */
    private void writePostfixOperator(int flags, OperatorEntry op, Atomic func, Term term) throws IOException {
        write(new OpCompare(op), flags, term);
        func.write(context);
    }

    /**
     * Write a variable from an integer term. Variable names are A...Z, A1...Z1, A2...Z2, etc.
     *
     * @param varNum Number of variable (0-based)
     * @throws IOException if IO error
     */
    private void writeVar(Term varNum) throws IOException {
        long vn = PrologInteger.from(varNum).toLong();
        char letter = (char) (vn % 26 + 'A');
        long num = vn / 26;
        String name;
        if (num == 0) {
            name = String.format("%c", letter);
        } else {
            name = String.format("%c%d", letter, num);
        }
        context.beginAlphaNum();
        output.write(name);
    }

    /**
     * Write a compound term structure in canonical form.
     *
     * @param term Term to write
     * @throws IOException on IO error
     */
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
            context.write(",");
            context.beginGraphic();
            write(comma, COMMA_UNSAFE, term.get(i));
        }
        context.beginSafe();
        context.write(")");
    }

    /**
     * Write a list. If the list looks like a string, this will format as a string rather than as a list.
     * @param term Term to convert to list.
     * @throws IOException on IO error.
     */
    private void writeSquareList(CompoundTerm term) throws IOException {
        if (term instanceof PrologChars) {
            term.write(context);
            return;
        }
        // First build up the list, analyzing what we expect to create
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
                    if (item instanceof PrologAtomLike) {
                        String name = ((PrologAtomLike) item).name();
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
            PrologChars cp = new PrologChars(builder.toString());
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

    private static class OpCompare {
        final OperatorEntry parent;

        OpCompare(OperatorEntry entry) {
            this.parent = entry;
        }

        boolean binaryBrackets(OperatorEntry op) {
            return parent.getPrecedence() <= op.getPrecedence();
        }

    }

    private static class LeftCompare extends OpCompare {

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

    private static class RightCompare extends OpCompare {

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
     * @param options     Options controlling formatting
     * @return String form
     */
    public static String toString(Environment environment, Term term, WriteOptions options) {
        try (StringOutputStream output = new StringOutputStream()) {
            WriteContext context = new WriteContext(
                    environment,
                    options,
                    output);
            StructureWriter structWriter = new StructureWriter(context);
            structWriter.write(term);
            return output.toString();
        } catch (IOException e) {
            throw new InternalError(e.getMessage(), e);
        }
    }
}
