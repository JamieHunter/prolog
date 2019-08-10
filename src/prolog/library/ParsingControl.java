// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Interned;
import prolog.bootstrap.Predicate;
import prolog.constants.PrologAtom;
import prolog.constants.PrologCharacter;
import prolog.constants.PrologInteger;
import prolog.exceptions.PrologPermissionError;
import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.execution.OperatorEntry;
import prolog.expressions.CompoundTerm;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;
import prolog.instructions.ExecFindCharConversion;
import prolog.instructions.ExecFindOp;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Predicates that control/modify parsing/compiling behavior are generally defined here
 */
public final class ParsingControl {
    private ParsingControl() {
        // Static methods/fields only
    }

    /**
     * Define/modify an operator
     *
     * @param environment Execution environment
     * @param precedence  New precedence
     * @param type        Operator type
     * @param name        Operator name
     */
    @Predicate("op")
    public static void op(Environment environment, Term precedence, Term type, Term name) {
        int precedenceInt = PrologInteger.from(precedence).get().intValue();
        PrologAtom nameAtom = PrologAtom.from(name);
        OperatorEntry.Code typeCode = OperatorEntry.parseCode(PrologAtom.from(type));
        boolean allowed = true;
        if (nameAtom == Interned.COMMA_FUNCTOR) {
            OperatorEntry existing = environment.getInfixPostfixOperator(Interned.COMMA_FUNCTOR);
            if (existing.getCode() != typeCode || existing.getPrecedence() != precedenceInt) {
                allowed = false;
            }
        } else if (nameAtom == Interned.BAR_FUNCTOR) {
            OperatorEntry existing = environment.getInfixPostfixOperator(Interned.BAR_FUNCTOR);
            if (existing.getCode() != typeCode || precedenceInt < 1001) {
                allowed = false;
            }
        } else if (nameAtom == Interned.EMPTY_BRACES_ATOM || nameAtom == Interned.EMPTY_LIST_ATOM) {
            allowed = false;
        }
        if (!allowed) {
            // TODO: Better error
            throw PrologPermissionError.error(environment, "modify", "op",
                    new CompoundTermImpl(environment.getAtom("op"), precedence, type, name),
                    "Modifying a restricted operator"
            );
        }
        environment.makeOperator(precedenceInt, typeCode, nameAtom);
    }

    /**
     * Query details of an operator/s
     *
     * @param compiling Compiling environment
     * @param compound  Term containing parameters
     */
    @Predicate(value = "current_op", arity = 3)
    public static void currentOp(CompileContext compiling, CompoundTerm compound) {
        // this is a search predicate, though rarely used to find more than one
        Term precedence = compound.get(0);
        Term type = compound.get(1);
        Term name = compound.get(2);
        compiling.add(new ExecFindOp(precedence, type, name));
    }

    /**
     * Character translation table, only active if flag is set. This is not supported
     * at this time.
     *
     * @param environment Execution environment
     * @param fromChar    Char to insert into table
     * @param toChar      Interpreted char
     */
    @Predicate("char_conversion")
    public static void charConversion(Environment environment, Term fromChar, Term toChar) {
        PrologCharacter source = PrologCharacter.from(environment, fromChar);
        PrologCharacter target = PrologCharacter.from(environment, toChar);
        environment.getCharConverter().add(source.get(), target.get());
    }

    /**
     * Query details of character conversions
     *
     * @param compiling Compiling environment
     * @param compound  Term containing parameters
     */
    @Predicate(value = "current_char_conversion", arity = 2)
    public static void currentCharConversion(CompileContext compiling, CompoundTerm compound) {
        // this is a search predicate, though rarely used to find more than one
        Term fromChar = compound.get(0);
        Term toChar = compound.get(1);
        compiling.add(new ExecFindCharConversion(fromChar, toChar));
    }
}
