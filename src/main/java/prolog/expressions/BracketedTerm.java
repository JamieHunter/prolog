// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.expressions;

import prolog.bootstrap.Interned;
import prolog.execution.EnumTermStrategy;
import prolog.execution.Environment;
import prolog.execution.LocalContext;
import prolog.execution.SimplifyTerm;
import prolog.io.WriteContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Given an expression of form (x) or (a,b,c) this temporarily represents it within a special container term.
 * In particular, this helps differentiate between (a,b,c) and (a,(b,c)) which cannot be done by simply representing
 * as CompoundTerms. During simplification, BracketedTerms are removed and never re-introduced.
 */
public final class BracketedTerm implements Term, Container {

    private final Term[] terms;

    /**
     * Create a BracketedTerm from a list of Term's
     *
     * @param terms List of terms
     */
    public BracketedTerm(List<Term> terms) {
        this.terms = terms.toArray(new Term[terms.size()]);
    }

    /**
     * Retrieve list of terms.
     *
     * @return list
     */
    public List<Term> get() {
        return Arrays.asList(terms);
    }

    /**
     * Expand into a tree of ','(left,right)
     *
     * @return Tree or single term
     */
    @Override
    public Term extract() {
        if (terms.length == 1) {
            // single term
            return terms[0];
        }
        // Build comma tree of terms
        CompoundTerm term = new CompoundTermImpl(Interned.COMMA_FUNCTOR,
                terms[terms.length - 2], terms[terms.length - 1]);
        for (int i = terms.length - 3; i >= 0; i--) {
            term = new CompoundTermImpl(Interned.COMMA_FUNCTOR, terms[i], term);
        }
        return term;
    }

    /**
     * When converted to a value, decompose into a real comma list.
     *
     * @param environment Environment for any environment-specific conversions
     * @return single term or comma tree
     */
    @Override
    public Term value(Environment environment) {
        return extract().value(environment);
    }

    /**
     * When resolving, decompose into a real comma list.
     *
     * @param context Local context for variable parsing
     * @return single term or comma tree
     */
    @Override
    public Term resolve(LocalContext context) {
        return extract().resolve(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Term enumTerm(EnumTermStrategy strategy) {
        return strategy.visitContainer(this);
    }

    /**
     * Write after converting into a comma-list first.
     *
     * @param context Write context
     * @throws IOException if IO Error
     */
    @Override
    public void write(WriteContext context) throws IOException {
        enumTerm(new SimplifyTerm(context.environment())).write(context);
    }

    /**
     * Convert to a string for debugging.
     *
     * @return Simple string
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('(');
        builder.append(terms[0].toString());
        for (int i = 1; i < terms.length; i++) {
            builder.append(',');
            builder.append(terms[i].toString());
        }
        builder.append(')');
        return builder.toString();
    }

}
