// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

import prolog.expressions.Term;
import prolog.variables.UnboundVariable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Context for copying a tree of terms
 */
public class CopyTermContext {
    private final Environment environment;
    private final Map<Term, Term> refMap = new IdentityHashMap<>();
    private final Map<Long, UnboundVariable> varMap = new HashMap<>();

    public CopyTermContext(Environment environment) {
        this.environment = environment;
    }

    public Environment environment() {
        return environment;
    }

    public Term copy(Term src, Function<? super Term, ? extends Term> mappingFunction) {
        return refMap.computeIfAbsent(src, mappingFunction);
    }

    public UnboundVariable var(String name, long id) {
        return varMap.computeIfAbsent(id, k -> new UnboundVariable(name, environment.nextVariableId()));
    }
}
