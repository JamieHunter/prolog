// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.Interned;
import prolog.bootstrap.Predicate;
import prolog.constants.Atomic;
import prolog.constants.PrologAtomInterned;
import prolog.constants.PrologCharacter;
import prolog.constants.PrologInteger;
import prolog.exceptions.PrologDomainError;
import prolog.exceptions.PrologInstantiationError;
import prolog.exceptions.PrologPermissionError;
import prolog.execution.CompileContext;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.execution.OperatorEntry;
import prolog.expressions.CompoundTerm;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;
import prolog.generators.YieldSolutions;
import prolog.instructions.ExecFindCharConversion;
import prolog.unification.Unifier;
import prolog.unification.UnifyBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        if (!precedence.isInstantiated()) {
            throw PrologInstantiationError.error(environment, precedence);
        }
        if (!type.isInstantiated()) {
            throw PrologInstantiationError.error(environment, type);
        }
        if (!name.isInstantiated()) {
            throw PrologInstantiationError.error(environment, name);
        }
        int precedenceInt = PrologInteger.from(precedence).toInteger();
        if (precedenceInt > 1200 || precedenceInt < 0) {
            throw PrologDomainError.operatorPriority(environment, precedence);
        }
        PrologAtomInterned nameAtom = PrologAtomInterned.from(environment, name);
        OperatorEntry.Code typeCode = OperatorEntry.parseCode(PrologAtomInterned.from(environment, type));
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
                    new CompoundTermImpl(environment.internAtom("op"), precedence, type, name),
                    "Modifying a restricted operator"
            );
        }
        if (precedenceInt > 0) {
            environment.makeOperator(precedenceInt, typeCode, nameAtom);
        } else {
            environment.removeOperator(typeCode, nameAtom);
        }
    }

    /**
     * Query details of an operator/s
     *
     * @param environment Execution environment
     * @param precedence  Operator precedence
     * @param type        Operator type
     * @param name        Operator name
     */
    @Predicate("current_op")
    public static void currentOp(Environment environment, Term precedence, Term type, Term name) {
        LocalContext context = environment.getLocalContext();

        PrologAtomInterned constrainedName = null;
        OperatorEntry.Code constrainedType = null;
        if (name.isInstantiated()) {
            // most significant constraint
            constrainedName = PrologAtomInterned.from(environment, name);
        }
        if (type.isInstantiated()) {
            // at best, constrains between two search lists
            constrainedType = OperatorEntry.parseCode(PrologAtomInterned.from(environment, type));
        }
        if (precedence.isInstantiated()) {
            // type check, but not processed until iteration
            PrologInteger.from(precedence).toInteger();
        }
        ArrayList<OperatorEntry> searchList = new ArrayList<>();
        // Apply constraints to search list. Ideally this will be one entry
        if (constrainedType != null) {
            if (constrainedType.isPrefix()) {
                addToOpList(searchList, constrainedName, environment.getPrefixOperators());
            } else {
                addToOpList(searchList, constrainedName, environment.getInfixPostfixOperators());
            }
        } else {
            addToOpList(searchList, constrainedName, environment.getPrefixOperators());
            addToOpList(searchList, constrainedName, environment.getInfixPostfixOperators());
        }
        Unifier precedenceUnifier = UnifyBuilder.from(precedence);
        Unifier typeUnifier = UnifyBuilder.from(type);
        Unifier nameUnifier = UnifyBuilder.from(name);
        YieldSolutions.forAll(environment, searchList.stream(), entry -> {
            PrologInteger entryPrecedence = new PrologInteger(entry.getPrecedence());
            PrologAtomInterned entryType = entry.getCode().atom();
            Atomic entryName = entry.getFunctor();

            return nameUnifier.unify(context, entryName) &&
                    typeUnifier.unify(context, entryType) &&
                    precedenceUnifier.unify(context, entryPrecedence);
        });
    }

    /**
     * Builds up a list ready for iteration.
     *
     * @param operators Target iteration list
     * @param name      Name of operator (filter)
     * @param source    Source map of operators
     */
    private static void addToOpList(List<OperatorEntry> operators, PrologAtomInterned name, Map<Atomic, OperatorEntry> source) {
        if (name != null) {
            OperatorEntry select = source.get(name);
            if (select != null) {
                operators.add(select);
            }
        } else {
            operators.addAll(source.values());
        }
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
        if (!fromChar.isInstantiated()) {
            throw PrologInstantiationError.error(environment, fromChar);
        }
        if (!toChar.isInstantiated()) {
            throw PrologInstantiationError.error(environment, toChar);
        }
        PrologCharacter source = PrologCharacter.from(environment, fromChar);
        PrologCharacter target = PrologCharacter.from(environment, toChar);
        environment.getCharConverter().add(source.get(), target.get());
    }

    /**
     * Query details of character conversions
     *
     * @param compiling Compiling environment
     * @param source    Term containing parameters
     */
    @Predicate(value = "current_char_conversion", arity = 2)
    public static void currentCharConversion(CompileContext compiling, CompoundTerm source) {
        // this is a search predicate, though rarely used to find more than one
        Term fromChar = source.get(0);
        Term toChar = source.get(1);
        compiling.add(source, new ExecFindCharConversion(fromChar, toChar));
    }
}
