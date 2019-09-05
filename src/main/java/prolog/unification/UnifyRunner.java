// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.unification;

import prolog.execution.LocalContext;
import prolog.expressions.Term;

import java.util.Collection;
import java.util.function.Function;

/**
 * Compiled unifier. Interprets a unification script to unify a target term with the compiled source term.
 */
public final class UnifyRunner implements Unifier {

    private final UnifyStep[] script;
    private final Function<Term, UnifyIterator> startIterator;

    /*package*/ UnifyRunner(Collection<UnifyStep> script, Function<Term, UnifyIterator> startIterator) {
        this.script = script.toArray(new UnifyStep[script.size()]);
        this.startIterator = startIterator;
    }

    /**
     * Run the unification script
     *
     * @param context Binding localContext to resolve variables
     * @param other   Term to be unified using this script
     * @return true if unification occurred, false otherwise
     */
    @Override
    public boolean unify(LocalContext context, Term other) {
        UnifyIterator it = startIterator.apply(other);
        for (UnifyStep entry : script) {
            it = entry.invoke(context, it);
            if (it.done()) {
                return it.success();
            }
        }
        return it.success();
    }

}
