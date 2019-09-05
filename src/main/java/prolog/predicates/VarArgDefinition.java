// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.predicates;

/**
 * Provide support for any var-arg definitions for a single functor.
 */
public class VarArgDefinition {
    private final Predication.Interned predication;
    private final PredicateDefinition definition;

    public VarArgDefinition(Predication.Interned predication, PredicateDefinition definition) {
        this.predication = predication;
        this.definition = definition;
    }

    /**
     * If there is a var-arg definition that satisfies the predication, return the definition
     *
     * @param predication Predication to look for
     * @return definition or null
     */
    public PredicateDefinition lookup(Predication.Interned predication) {
        // assume the functor is already tested
        // defined like this to later support multiple vararg variants if needed
        if (predication.arity() <= predication.arity()) {
            return definition;
        }
        return null;
    }
}
