// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.constants.PrologCharacter;
import prolog.debugging.InstructionReflection;
import prolog.execution.DecisionPoint;
import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.execution.LocalContext;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.parser.CharConverter;

/**
 * Find and enumerate character conversions
 */
public class ExecFindCharConversion extends Traceable {
    private final Term fromTerm;
    private final Term toTerm;

    public ExecFindCharConversion(CompoundTerm source, Term fromTerm, Term toTerm) {
        super(source);
        this.fromTerm = fromTerm;
        this.toTerm = toTerm;
    }

    /**
     * Iterate first conversion in list of conversions
     *
     * @param environment Execution environment
     */
    @Override
    public void invoke(Environment environment) {
        LocalContext context = environment.getLocalContext();
        Term boundFrom = fromTerm.resolve(context);
        Term boundTo = toTerm.resolve(context);

        Character constrainedFrom = null;
        Character constrainedTo = null;
        if (boundFrom.isInstantiated()) {
            // most significant constraint
            constrainedFrom = PrologCharacter.from(environment, boundFrom).get();
        }
        if (boundTo.isInstantiated()) {
            // scan constraint
            constrainedTo = PrologCharacter.from(environment, boundTo).get();
        }
        CharConverter converter = environment.getCharConverter();
        converter.initialize(); // this might be first time converter table is used
        int start = 0;
        int end = converter.getSize();
        if (constrainedFrom != null) {
            // single table entry
            start = constrainedFrom;
            end = start + 1;
        }
        TableIterator iter =
                new TableIterator(environment, converter, start, end, constrainedFrom, constrainedTo, boundFrom, boundTo);
        iter.next();
    }

    /**
     * Operator iterator decision point.
     */
    private static class TableIterator extends DecisionPoint {

        final CharConverter converter;
        final int limit;
        final Term boundFrom;
        final Term boundTo;
        final Character charFrom;
        final Character charTo;
        int index;

        TableIterator(Environment environment,
                      CharConverter converter,
                      int start,
                      int limit,
                      Character charFrom,
                      Character charTo,
                      Term boundFrom,
                      Term boundTo) {
            super(environment);
            this.converter = converter;
            this.index = start;
            this.limit = limit;
            this.charFrom = charFrom;
            this.charTo = charTo;
            this.boundFrom = boundFrom;
            this.boundTo = boundTo;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void next() {
            if (charTo != null) {
                // scan forward (this means we will have a match, or will have hit limit)
                index = converter.scan(index, limit, charTo);
            }
            if (index == limit) {
                // final fail
                environment.backtrack();
                return;
            }
            //
            // Above constraint logic means that this must be a success
            // Case 1 - from & to characters defined - scan a single character, test second via scan
            // Case 2 - from defined, to undefined - iterate a single character
            // Case 3 - to character defined - scan for single character, hit limit if not defined
            // Case 4 - neither defined - iterate a single character
            //
            char entryFrom = (char) index++;
            char entryTo = converter.convert(entryFrom);
            if (index != limit) {
                // Backtracking will try another character
                // decision point must be added before the unification
                environment.pushDecisionPoint(this);
            }
            // unify, full unify logic not needed here, and this approach works better
            // for this specific use-case because of the number of valid character specifiers
            if (charFrom == null) {
                boundFrom.instantiate(new PrologCharacter(entryFrom));
            }
            if (charTo == null) {
                boundTo.instantiate(new PrologCharacter(entryTo));
            }
            environment.forward();
        }
    }
}
